package app.facerec;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.util.*;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceAuth {
    private final LBPHFaceRecognizer lbph;
    private final FaceService faceService;
    private final double threshold; // umbral de aceptación (p.ej. 45–50)

    // Parámetros internos de robustez (puedes ajustar)
    private static final int MIN_VOTES = 10;       // mínimo de “coincidencias” del mismo id
    private static final double VOTE_MARGIN = 10;  // margen para contar votos (threshold + margen)

    public FaceAuth(String modelPath, String cascadePath, double threshold) {
        this.lbph = LBPHFaceRecognizer.create();
        this.lbph.read(modelPath);
        this.faceService = new FaceService(cascadePath);
        this.threshold = threshold;
    }

    /**
     * Escanea 'framesToScan' frames de la cámara 'cameraIndex'.
     * Usa votación por id y mediana de confianza.
     * Devuelve idUsuario si pasa criterios; si no, null.
     */
    public Integer predictUserIdFromWebcam(int cameraIndex, int framesToScan) {
        VideoCapture cam = new VideoCapture(cameraIndex);
        if (!cam.isOpened()) throw new RuntimeException("No se pudo abrir la webcam índice " + cameraIndex);

        try {
            Map<Integer, List<Double>> confByLabel = new HashMap<>();
            Mat frame = new Mat();

            int frames = 0;
            while (frames++ < Math.max(5, framesToScan)) {
                if (!cam.read(frame) || frame.empty()) continue;

                RectVector faces = faceService.detectFaces(frame);
                for (int i = 0; i < faces.size(); i++) {
                    Rect r = faces.get(i);
                    Mat face = new Mat(frame, r);
                    Mat gray = new Mat();
                    cvtColor(face, gray, COLOR_BGR2GRAY);
                    resize(gray, gray, new Size(200, 200));

                    int[] label = new int[1];
                    double[] conf = new double[1];
                    lbph.predict(gray, label, conf);

                    // Solo contamos votos si la confianza está cerca del umbral
                    if (conf[0] <= (threshold + VOTE_MARGIN)) {
                        confByLabel.computeIfAbsent(label[0], k -> new ArrayList<>()).add(conf[0]);
                    }
                }
            }

            if (confByLabel.isEmpty()) return null;

            // Elegimos el mejor id: más votos y menor mediana de confianza
            Integer bestLabel = confByLabel.entrySet().stream()
                    .sorted((a, b) -> {
                        int byVotes = Integer.compare(b.getValue().size(), a.getValue().size());
                        if (byVotes != 0) return byVotes;
                        return Double.compare(median(a.getValue()), median(b.getValue()));
                    })
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (bestLabel == null) return null;

            List<Double> confs = confByLabel.get(bestLabel);
            int votes = confs.size();
            double med = median(confs);

            System.out.println("Predicción: label=" + bestLabel + " votos=" + votes +
                    " medianaConf=" + String.format(java.util.Locale.US, "%.2f", med));

            // Criterios de aceptación
            if (votes >= MIN_VOTES && med <= threshold) {
                return bestLabel;
            }
            return null;

        } finally {
            cam.release();
        }
    }

    private static double median(List<Double> xs) {
        if (xs == null || xs.isEmpty()) return Double.POSITIVE_INFINITY;
        List<Double> s = xs.stream().sorted().collect(Collectors.toList());
        int n = s.size();
        return (n % 2 == 1) ? s.get(n / 2) : (s.get(n / 2 - 1) + s.get(n / 2)) / 2.0;
    }
}