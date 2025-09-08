package app.view;

import app.core.Sesion;
import app.dao.UsuarioDAO;
import app.model.Usuario;
import app.facerec.FaceAuth;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LoginReconForm {
    public JPanel panelPrincipal;
    private JButton btnLoginFacial;
    private JButton btnVolver;
    private JLabel lblStatus;

    // Recurso dentro de src/main/resources
    private static final String CASCADE_RESOURCE = "haarcascades/haarcascade_frontalface_default.xml";

    // Modelo entrenado (en carpeta de datos, fuera de resources)
    private static final String MODEL_REL = "data/modelos/lbph_model.xml";

    private static final double THRESHOLD   = 75.0; // ajustar en pruebas
    private static final int CAMERA_INDEX   = 0;
    private static final int FRAMES_TO_SCAN = 30;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public LoginReconForm() {
        panelPrincipal.setPreferredSize(new Dimension(420, 200));
        setStatus("Listo para reconocimiento facial");

        btnLoginFacial.addActionListener(e -> onLoginFacial());
        if (btnVolver != null) btnVolver.addActionListener(e -> cerrar());
    }

    private void onLoginFacial() {
        setStatus("Preparando reconocimiento...");

        try {
            double THRESHOLD = 45.0; // más estricto
            int CAMERA_INDEX = 0;
            int FRAMES_TO_SCAN = 40; // más frames para mejor votación

            String modelPath   = resolvePath(MODEL_REL);
            String cascadePath = getCascadePathFromResources();

            FaceAuth auth = new FaceAuth(modelPath, cascadePath, THRESHOLD);
            setStatus("Abriendo cámara y analizando...");
            Integer userId = auth.predictUserIdFromWebcam(CAMERA_INDEX, FRAMES_TO_SCAN);

            if (userId == null) { setStatus("No se pudo reconocer el rostro."); return; }

            // Reutilizas tu mismo DAO/flow ya existente
            Usuario u = usuarioDAO.buscarPorId(userId);
            if (u == null || u.getEstado() != 1) { setStatus("Usuario no válido o inactivo."); return; }

            // ¡Listo! Esto ya arrastra rol y todo lo demás que ya tienes montado
            Sesion.login(u);

            setStatus("Bienvenido " + (u.getNombre()!=null?u.getNombre():u.getUsername()));
            abrirMenu();
            cerrar();

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Error: " + ex.getMessage());
        }
    }

    /** Convierte una ruta relativa (respecto al working directory) a absoluta y la imprime para depurar. */
    private String resolvePath(String rel) {
        String wd = System.getProperty("user.dir");
        File f = new File(wd, rel);
        System.out.println("Modelo LBPH (resuelto): " + f.getAbsolutePath());
        return f.getAbsolutePath();
    }

    /** Toma el cascade desde resources y lo copia a un archivo temporal en disco (OpenCV requiere path físico). */
    private String getCascadePathFromResources() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CASCADE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + CASCADE_RESOURCE + " en src/main/resources");
            }
            var temp = Files.createTempFile("cascade_", ".xml");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toFile().getAbsolutePath();
        }
    }

    private void abrirMenu() {
        JFrame f = new JFrame("Menú Principal – Librería");
        f.setContentPane(new MainMenuForm().panelPrincipal);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private void cerrar() {
        Window w = SwingUtilities.getWindowAncestor(panelPrincipal);
        if (w != null) w.dispose();
    }

    private void setStatus(String s) {
        if (lblStatus != null) lblStatus.setText(s);
    }

    // Launcher opcional para probar solo este form
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Login Facial");
            f.setContentPane(new LoginReconForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}