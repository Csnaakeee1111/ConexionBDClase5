package app.view;

import app.facerec.FaceService;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_ANY;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_AVFOUNDATION;

public class CapturaRostrosForm {
    public JPanel panelPrincipal;
    private JTextField txtIdUsuario;
    private JSpinner spnMuestras;
    private JButton btnCapturar;
    private JButton btnCerrar;
    private JLabel lblStatus;
    private JLabel lblPreview; // <- preview en vivo

    // BACKEND recomendado en mac (si no, usa CAP_ANY)
    private static final int BACKEND = CAP_AVFOUNDATION;
    private static final int CAMERA_INDEX = 0; // si no detecta, prueba 1
    private volatile boolean running = false;
    // en CapturaRostrosForm.java (cabecera de la clase)
    private Integer forcedUserId = null;

    // NUEVO constructor: recibe el id y lo coloca en el txtIdUsuario (bloqueado)
    public CapturaRostrosForm(int userId) {
        this(); // llama al constructor default para armar la UI
        this.forcedUserId = userId;
        if (txtIdUsuario != null) {
            txtIdUsuario.setText(String.valueOf(userId));
            txtIdUsuario.setEditable(false);
        }
    }
    public CapturaRostrosForm() {
        panelPrincipal.setPreferredSize(new Dimension(900, 680));
        // tamaño sugerido de la “ventana” de preview
        lblPreview.setPreferredSize(new Dimension(640, 480));
        lblPreview.setMinimumSize(new Dimension(320, 240));
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);

        if (spnMuestras.getModel() == null) {
            spnMuestras.setModel(new SpinnerNumberModel(40, 5, 500, 1));
        }
        setStatus("Listo para capturar rostros");

        btnCapturar.addActionListener(e -> {
            if (running) {
                running = false; // toggle para detener
                btnCapturar.setText("Capturar");
                setStatus("Captura detenida");
            } else {
                startCapture();
            }
        });

        btnCerrar.addActionListener(e -> {
            running = false;
            Window w = SwingUtilities.getWindowAncestor(panelPrincipal);
            if (w != null) w.dispose();
        });
    }

    private void startCapture() {
        int idUsuario;
        if (forcedUserId != null) {
            idUsuario = forcedUserId;
        } else {
            String idTxt = txtIdUsuario.getText().trim();
            if (idTxt.isEmpty()) { setStatus("Ingrese idUsuario (entero)"); return; }
            try { idUsuario = Integer.parseInt(idTxt); }
            catch (NumberFormatException ex) { setStatus("idUsuario inválido"); return; }
        }

        int muestras = (Integer) spnMuestras.getValue();
        running = true;
        btnCapturar.setText("Detener");
        setStatus("Abriendo cámara...");

        new Thread(() -> {
            try {
                FaceService faceService = new FaceService(getCascadePath());
                VideoCapture cam = new VideoCapture(CAMERA_INDEX, BACKEND);
                if (!cam.isOpened()) {
                    // fallback a cualquier backend
                    cam = new VideoCapture(CAMERA_INDEX, CAP_ANY);
                    if (!cam.isOpened()) {
                        setStatus("No se pudo abrir la webcam");
                        running = false;
                        return;
                    }
                }

                // Warm-up ~500ms
                Mat frame = new Mat();
                long t0 = System.currentTimeMillis();
                while (System.currentTimeMillis() - t0 < 500 && running) {
                    cam.read(frame);
                }

                File outDir = Paths.get("data", "faces", String.valueOf(idUsuario)).toFile();
                if (!outDir.exists()) outDir.mkdirs();

                int count = 0;
                int loops = 0;

                while (running && count < muestras) {
                    if (!cam.read(frame) || frame.empty()) continue;

                    // Detección con minSize relativo al frame (mas sensible)
                    int minSide = Math.min(frame.cols(), frame.rows());
                    int minSize = Math.max(60, (int)(minSide * 0.18)); // 18% del lado mínimo
                    RectVector faces = faceService.detectFaces(frame, new Size(minSize, minSize));

                    // Dibujar preview
                    Mat preview = frame.clone();
                    for (int i = 0; i < faces.size(); i++) {
                        Rect r = faces.get(i);
                        rectangle(preview, r, new Scalar(0, 255, 0, 0), 2, LINE_8, 0);
                    }
                    updatePreview(preview);

                    if (faces.size() == 0) {
                        // Guarda frame de depuración cada 30 intentos para ver qué llega
                        if ((++loops % 30) == 0) {
                            Mat grayDbg = new Mat();
                            cvtColor(frame, grayDbg, COLOR_BGR2GRAY);
                            resize(grayDbg, grayDbg, new Size(200, 200));
                            String dbg = new File(outDir, "u" + idUsuario + "_nodet_" + System.currentTimeMillis() + ".png").getAbsolutePath();
                            imwrite(dbg, grayDbg);
                            setStatus("Sin rostro detectado (guardado debug). Iluminación/frontalidad?");
                        }
                        Thread.sleep(10);
                        continue;
                    }

                    for (int i = 0; i < faces.size(); i++) {
                        Rect r = faces.get(i);
                        Mat face = new Mat(frame, r);
                        Mat gray = new Mat();
                        cvtColor(face, gray, COLOR_BGR2GRAY);
                        resize(gray, gray, new Size(200, 200));

                        String filename = new File(outDir, "u" + idUsuario + "_" + System.currentTimeMillis() + ".png").getAbsolutePath();
                        imwrite(filename, gray);
                        count++;
                        setStatus("Guardadas: " + count + " / " + muestras);
                        if (count >= muestras) break;
                    }

                    Thread.sleep(10);
                }

                // justo después de cam.release(); y antes/depués del setStatus final
                cam.release();
                setStatus("Captura completa: " + count + " imágenes. Entrenando modelo...");

// Entrenar en segundo plano
                new Thread(() -> {
                    try {
                        // mismas rutas que ya usas en todo el proyecto
                        String facesRoot = "data/faces";
                        String modelPath = "data/modelos/lbph_model.xml";

                        app.facerec.FaceTrainer.train(facesRoot, modelPath);

                        SwingUtilities.invokeLater(() -> {
                            setStatus("✅ Entrenamiento finalizado. Modelo listo.");
                            JOptionPane.showMessageDialog(panelPrincipal,
                                    "Entrenamiento finalizado.\nModelo: " + new java.io.File(modelPath).getAbsolutePath(),
                                    "Reconocimiento facial", JOptionPane.INFORMATION_MESSAGE);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            setStatus("❌ Error entrenando: " + ex.getMessage());
                            JOptionPane.showMessageDialog(panelPrincipal,
                                    "Error entrenando: " + ex.getMessage(),
                                    "Reconocimiento facial", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
                running = false;
                SwingUtilities.invokeLater(() -> btnCapturar.setText("Capturar"));
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error: " + ex.getMessage());
                running = false;
                SwingUtilities.invokeLater(() -> btnCapturar.setText("Capturar"));
            }

        }).start();

    }

    private void updatePreview(Mat bgr) {
        BufferedImage src = matToBufferedImage(bgr);
        if (src == null) return;

        int targetW = lblPreview.getWidth()  > 0 ? lblPreview.getWidth()  : lblPreview.getPreferredSize().width;
        int targetH = lblPreview.getHeight() > 0 ? lblPreview.getHeight() : lblPreview.getPreferredSize().height;

        BufferedImage scaled = scaleToFit(src, targetW, targetH); // mantiene proporción
        SwingUtilities.invokeLater(() -> lblPreview.setIcon(new ImageIcon(scaled)));
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) return null;
        Mat rgb = new Mat();
        // BGR -> RGB
        cvtColor(mat, rgb, COLOR_BGR2RGB);
        int w = rgb.cols(), h = rgb.rows(), ch = rgb.channels();
        byte[] data = new byte[w * h * ch];
        rgb.data().get(data);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        // OJO: TYPE_3BYTE_BGR espera BGR; ya convertimos a RGB, pero el raster es byte-aligned.
        // Para mostrar correctamente: escribimos los bytes tal cual y se verá bien en Swing.
        image.getRaster().setDataElements(0, 0, w, h, data);
        return image;
    }

    /** Escala manteniendo proporción para que quepa en targetW x targetH (letterbox). */
    private BufferedImage scaleToFit(BufferedImage src, int targetW, int targetH) {
        double arSrc = (double) src.getWidth() / src.getHeight();
        double arDst = (double) targetW / targetH;

        int w, h;
        if (arSrc > arDst) {
            // limitado por ancho
            w = targetW;
            h = (int) Math.round(targetW / arSrc);
        } else {
            // limitado por alto
            h = targetH;
            w = (int) Math.round(targetH * arSrc);
        }

        BufferedImage dst = new BufferedImage(targetW, targetH, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = dst.createGraphics();
        g.setColor(Color.BLACK);               // bordes (letterbox)
        g.fillRect(0, 0, targetW, targetH);
        // dibujar centrado
        int x = (targetW - w) / 2;
        int y = (targetH - h) / 2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, x, y, w, h, null);
        g.dispose();
        return dst;
    }

    private void setStatus(String s) {
        SwingUtilities.invokeLater(() -> { if (lblStatus != null) lblStatus.setText(s); });
    }

    // Cargar cascade desde resources -> archivo temporal en disco
    private String getCascadePath() {
        String resourceName = "haarcascades/haarcascade_frontalface_default.xml";
        try (var in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new IllegalStateException("No se encontró " + resourceName + " en resources");
            java.nio.file.Path temp = java.nio.file.Files.createTempFile("cascade_", ".xml");
            java.nio.file.Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toFile().getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo preparar el cascade: " + e.getMessage(), e);
        }
    }
    // Launcher opcional para probar solo este form
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Captura de Rostros");
            f.setContentPane(new CapturaRostrosForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}