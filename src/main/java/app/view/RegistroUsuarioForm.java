package app.view;

import app.dao.UsuarioDAO;
import app.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RegistroUsuarioForm {
    public JPanel panelPrincipal;

    private JTextField txtUsername;
    private JTextField txtNombre;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirm;
    private JCheckBox chkMostrar;
    private JComboBox<String> cboRol;     // ADMIN / OPERADOR
    private JComboBox<String> cboEstado;  // "1 - Activo" / "0 - Inactivo"

    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnCargar;

    private JTable tblUsuarios;

    // opcional: ponlo en el .form si quieres mensajes; si no, déjalo y no lo dibujas
    private JLabel lblStatus;
    private JButton btnCapturaRostro;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Username", "Nombre", "Rol", "Estado"}, 0
    );

    private Integer selectedId = null;
    private char echoPass, echoConf;

    public RegistroUsuarioForm() {
        panelPrincipal.setPreferredSize(new Dimension(900, 600));

        tblUsuarios.setModel(model);
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // combos
        cboRol.addItem("ADMIN");
        cboRol.addItem("OPERADOR");

        cboEstado.addItem("1 - Activo");
        cboEstado.addItem("0 - Inactivo");

        // show/hide pass
        echoPass = txtPassword.getEchoChar();
        echoConf = txtConfirm.getEchoChar();
        chkMostrar.addActionListener(e -> togglePasswordEcho());

        // acciones
        btnGuardar.addActionListener(e -> onGuardar());
        btnActualizar.addActionListener(e -> onActualizar());
        btnCargar.addActionListener(e -> cargarTabla());
        btnCapturaRostro.addActionListener(e -> abrirCapturaRostro());

        // selección de tabla
        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblUsuarios.getSelectedRow();
            if (row == -1) {
                selectedId = null;
                txtUsername.setEnabled(true);
                limpiarPassword();
                setStatus(" ");
                return;
            }
            selectedId = Integer.parseInt(model.getValueAt(row, 0).toString());
            txtUsername.setText(model.getValueAt(row, 1).toString());
            txtNombre.setText(model.getValueAt(row, 2).toString());
            cboRol.setSelectedItem(model.getValueAt(row, 3).toString());
            cboEstado.setSelectedIndex(
                    "Activo".equalsIgnoreCase(model.getValueAt(row, 4).toString()) ? 0 : 1
            );

            // en edición no cambiamos username
            txtUsername.setEnabled(false);
            limpiarPassword(); // vacía pass/confirm
            setStatus("Modo edición");
        });

        // estado inicial
        modoNuevo();
        cargarTabla();
    }
    private Integer getIdActual() {
        try {
            String s = selectedId.toString().trim();   // <-- usa el nombre real de tu campo id
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void abrirCapturaRostro() {
        // 1) Obtener id actual (ajusta según tu caso: txtId o JTable)
        Integer userId = getIdActual();         // O usa getIdActualDesdeTabla();

        // 2) Si no hay id, pide guardar/cargar primero
        if (userId == null) {
            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Primero guarda o carga un usuario para obtener su ID.",
                    "Captura de Rostros",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // 3) Abrir el formulario de captura pasando el id
        JFrame f = new JFrame("Captura de Rostros — Usuario " + userId);
        f.setContentPane(new app.view.CapturaRostrosForm(userId).panelPrincipal);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private void togglePasswordEcho() {
        boolean show = chkMostrar.isSelected();
        txtPassword.setEchoChar(show ? (char)0 : echoPass);
        txtConfirm.setEchoChar(show ? (char)0 : echoConf);
    }

    private void setStatus(String s) {
        if (lblStatus != null) lblStatus.setText(s);
    }

    private void modoNuevo() {
        selectedId = null;
        txtUsername.setEnabled(true);
        txtUsername.setText("");
        txtNombre.setText("");
        cboRol.setSelectedIndex(0);
        cboEstado.setSelectedIndex(0);
        limpiarPassword();
        tblUsuarios.clearSelection();
        setStatus("Modo nuevo");
    }

    private void limpiarPassword() {
        txtPassword.setText("");
        txtConfirm.setText("");
    }

    private void cargarTabla() {
        try {
            List<Usuario> lista = usuarioDAO.listar(null); // sin filtro, como Autor/Libro
            model.setRowCount(0);
            for (Usuario u : lista) {
                model.addRow(new Object[]{
                        u.getId(),
                        u.getUsername(),
                        u.getNombre(),
                        u.getRol(),
                        (u.getEstado() == 1 ? "Activo" : "Inactivo")
                });
            }
            setStatus("Filas: " + model.getRowCount());
        } catch (Exception ex) {
            setStatus("Error al cargar");
            ex.printStackTrace();
        }
    }

    // ==== CREATE ====
    private void onGuardar() {
        String username = txtUsername.getText().trim();
        String nombre   = txtNombre.getText().trim();
        String rol      = (String) cboRol.getSelectedItem();
        int    estado   = (cboEstado.getSelectedIndex() == 0) ? 1 : 0;

        String pass     = new String(txtPassword.getPassword());
        String confirm  = new String(txtConfirm.getPassword());

        if (username.length() < 3) { setStatus("Usuario mínimo 3 caracteres"); txtUsername.requestFocus(); return; }
        if (nombre.isEmpty())      { setStatus("Ingrese el nombre"); txtNombre.requestFocus(); return; }
        if (pass.length() < 6)     { setStatus("Contraseña mínima 6"); txtPassword.requestFocus(); return; }
        if (!pass.equals(confirm)) { setStatus("Las contraseñas no coinciden"); txtConfirm.requestFocus(); return; }

        try {
            if (usuarioDAO.existsUsername(username)) {
                setStatus("El usuario ya existe");
                txtUsername.requestFocus();
                return;
            }
            int id = usuarioDAO.crearUsuario(username, pass, nombre, rol, estado);
            if (id > 0) {
                setStatus("Creado (id=" + id + ")");
                selectedId = id;
                cargarTabla();
                modoNuevo();
                // (opcional) seleccionarFilaPorId(id);
            } else {
                setStatus("No se pudo crear");
            }
        } catch (Exception ex) {
            setStatus("Error al crear");
            ex.printStackTrace();
        }
    }

    // ==== UPDATE (perfil + delete lógico a través de 'estado') ====
    private void onActualizar() {
        if (selectedId == null) { setStatus("Seleccione un registro"); return; }

        String nombre = txtNombre.getText().trim();
        String rol    = (String) cboRol.getSelectedItem();
        int estado    = (cboEstado.getSelectedIndex() == 0) ? 1 : 0;

        if (nombre.isEmpty()) { setStatus("Ingrese el nombre"); txtNombre.requestFocus(); return; }

        try {
            boolean ok = usuarioDAO.actualizarPerfil(selectedId, nombre, rol, estado);
            if (!ok) { setStatus("No se actualizó perfil"); return; }

            // cambiar password SOLO si se llenan ambos campos
            String pass    = new String(txtPassword.getPassword());
            String confirm = new String(txtConfirm.getPassword());
            if (!pass.isBlank() || !confirm.isBlank()) {
                if (pass.length() < 6) { setStatus("Contraseña mínima 6"); txtPassword.requestFocus(); return; }
                if (!pass.equals(confirm)) { setStatus("Las contraseñas no coinciden"); return; }
                boolean okPass = usuarioDAO.cambiarPassword(selectedId, pass);
                if (!okPass) { setStatus("No se actualizó password"); return; }
            }

            setStatus("Actualizado");
            cargarTabla();
            // mantener en edición el mismo registro
            // seleccionarFilaPorId(selectedId); // si lo quieres, implementa como en Autor/Libro
        } catch (Exception ex) {
            setStatus("Error al actualizar");
            ex.printStackTrace();
        }
    }

    // Launcher individual (opcional)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Usuarios – CRUD");
            f.setContentPane(new RegistroUsuarioForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}