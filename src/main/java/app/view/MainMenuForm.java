// src/main/java/app/view/MainMenuForm.java
package app.view;

import app.core.Sesion;
import javax.swing.*;
import java.awt.*;

public class MainMenuForm {
    public JPanel panelPrincipal;
    private JLabel lblUsuario;
    private JButton btnAutores;
    private JButton btnLibros;
    private JButton btnRegistrarUsuario; // abre RegistroUsuario
    private JButton btnSalir;

    public MainMenuForm() {
        panelPrincipal.setPreferredSize(new Dimension(420, 260));

        if (Sesion.isLogged() && lblUsuario != null) {
            lblUsuario.setText("Usuario: " + Sesion.getUsuario().getNombre()
                    + " (" + Sesion.getUsuario().getRol() + ")");
        }

        boolean esAdmin = Sesion.hasRole("ADMIN");
        if (btnRegistrarUsuario != null) {
            btnRegistrarUsuario.setEnabled(esAdmin);
            // si prefieres ocultarlo para OPERADOR:
            // btnRegistrarUsuario.setVisible(esAdmin);
        }

        if (btnAutores != null)  btnAutores.addActionListener(e -> abrirAutores());
        if (btnLibros  != null)  btnLibros.addActionListener(e -> abrirLibros());
        if (btnRegistrarUsuario != null) btnRegistrarUsuario.addActionListener(e -> abrirRegistroUsuario());
        if (btnSalir   != null)  btnSalir.addActionListener(e -> {
            Sesion.logout();
            System.exit(0);
        });
    }

    private void abrirAutores() {
        JFrame f = new JFrame("Gestión de Autores");
        f.setContentPane(new AutorForm().panelPrincipal);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.pack(); f.setLocationRelativeTo(null); f.setVisible(true);
    }

    private void abrirLibros() {
        JFrame f = new JFrame("Gestión de Libros");
        f.setContentPane(new LibroForm().panelPrincipal);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.pack(); f.setLocationRelativeTo(null); f.setVisible(true);
    }

    private void abrirRegistroUsuario() {
        if (!Sesion.hasRole("ADMIN")) return;
        JFrame f = new JFrame("Registro de Usuario");
        f.setContentPane(new RegistroUsuarioForm().panelPrincipal); // <- NOMBRE CORRECTO
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.pack(); f.setLocationRelativeTo(null); f.setVisible(true);
    }

    // launcher opcional
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Menú Principal – Librería");
            f.setContentPane(new MainMenuForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack(); f.setLocationRelativeTo(null); f.setVisible(true);
        });
    }
}