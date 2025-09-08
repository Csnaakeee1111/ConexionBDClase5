package app.view;

import app.dao.UsuarioDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginForm {
    public JPanel panelPrincipal;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnEntrar;
    private JLabel lblStatus;
    private JButton btnLoginFacial;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public LoginForm() {
        panelPrincipal.setPreferredSize(new Dimension(360, 200));
        btnEntrar.addActionListener(e -> onEntrar());
        btnLoginFacial.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame f = new JFrame("Login Facial");
                f.setContentPane(new LoginReconForm().panelPrincipal);
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                f.pack(); f.setLocationRelativeTo(null); f.setVisible(true);
            }
        });
    }

    // dentro de LoginForm.java
    private void onEntrar() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            if (lblStatus != null) lblStatus.setText("Ingrese usuario y contraseña");
            return;
        }
        try {
            app.model.Usuario u = new app.dao.UsuarioDAO().validarLogin(user, pass); // <- ESTE método
            if (u == null) {
                if (lblStatus != null) lblStatus.setText("Credenciales inválidas");
                return;
            }
            app.core.Sesion.login(u);
            abrirMenu();
            javax.swing.SwingUtilities.getWindowAncestor(panelPrincipal).dispose();
        } catch (Exception ex) {
            if (lblStatus != null) lblStatus.setText("Error de conexión");
            ex.printStackTrace();
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

    // Launcher
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Login");
            f.setContentPane(new LoginForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}