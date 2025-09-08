package app.view;

import app.dao.AutorDAO;
import app.model.Autor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AutorForm {
    public JPanel panelPrincipal;
    private JTextField txtNombre;
    private JTextArea txtBiografia;
    private JComboBox<String> cboEstado; // "1 - Activo" / "0 - Inactivo"
    private JButton btnGuardar;
    private JButton btnCargar;
    private JTable tblAutores;
    private JButton btnActualizar; // <-- nuevo botón

    private final AutorDAO autorDAO = new AutorDAO();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Biografía", "Estado"}, 0
    );

    // guardamos el ID seleccionado para actualizar
    private Integer selectedId = null;

    public AutorForm() {
        panelPrincipal.setPreferredSize(new Dimension(900, 600));

        tblAutores.setModel(model);
        tblAutores.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Combo de estado
        cboEstado.addItem("1 - Activo");
        cboEstado.addItem("0 - Inactivo");
        cboEstado.setSelectedIndex(0);

        // Listeners
        btnGuardar.addActionListener(e -> onGuardar());
        btnCargar.addActionListener(e -> cargarTabla());
        btnActualizar.addActionListener(e -> onActualizar());

        // Cuando selecciono una fila, cargo los datos en los campos
        tblAutores.getSelectionModel().addListSelectionListener(this::onTableSelection);
    }

    private void onTableSelection(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return; // evitar doble disparo
        int row = tblAutores.getSelectedRow();
        if (row == -1) {
            selectedId = null;
            return;
        }
        // Leemos desde el modelo de la tabla
        Object idVal   = model.getValueAt(row, 0);
        Object nomVal  = model.getValueAt(row, 1);
        Object bioVal  = model.getValueAt(row, 2);
        Object estVal  = model.getValueAt(row, 3); // "Activo"/"Inactivo" o 1/0 según como llenes

        // Guardamos el id seleccionado
        selectedId = (idVal != null) ? Integer.parseInt(idVal.toString()) : null;

        // Cargamos los campos
        txtNombre.setText(nomVal != null ? nomVal.toString() : "");
        txtBiografia.setText(bioVal != null ? bioVal.toString() : "");

        // Ajusta estado en combo
        // Si en la tabla pusiste "Activo"/"Inactivo":
        if (estVal != null && estVal.toString().equalsIgnoreCase("Activo")) {
            cboEstado.setSelectedIndex(0); // 1
        } else if (estVal != null && estVal.toString().equalsIgnoreCase("Inactivo")) {
            cboEstado.setSelectedIndex(1); // 0
        } else {
            // Si prefieres cargar 1/0 directo en la tabla, haz:
            // int est = Integer.parseInt(estVal.toString());
            // cboEstado.setSelectedIndex(est == 1 ? 0 : 1);
        }
    }

    private void onGuardar() {
        String nombre = txtNombre.getText().trim();
        String bio    = txtBiografia.getText().trim();
        int estado    = (cboEstado.getSelectedIndex() == 0) ? 1 : 0;

        if (nombre.isEmpty()) {
            txtNombre.requestFocus();
            txtNombre.setToolTipText("El nombre es obligatorio");
            return;
        }

        try {
            Autor a = new Autor(nombre, bio, estado);
            autorDAO.insertar(a);
            limpiarFormulario();
            cargarTabla();
            selectedId = null; // reseteo selección
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onActualizar() {
        if (selectedId == null) {
            // No hay fila seleccionada
            return;
        }
        String nombre = txtNombre.getText().trim();
        String bio    = txtBiografia.getText().trim();
        int estado    = (cboEstado.getSelectedIndex() == 0) ? 1 : 0;

        if (nombre.isEmpty()) {
            txtNombre.requestFocus();
            txtNombre.setToolTipText("El nombre es obligatorio");
            return;
        }

        try {
            Autor a = new Autor(selectedId, nombre, bio, estado);
            boolean ok = autorDAO.actualizar(a);
            if (ok) {
                cargarTabla();
                // Mantener selección: opcional
                seleccionarFilaPorId(selectedId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void seleccionarFilaPorId(Integer id) {
        if (id == null) return;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object val = model.getValueAt(i, 0);
            if (val != null && Integer.parseInt(val.toString()) == id) {
                tblAutores.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void cargarTabla() {
        try {
            List<Autor> lista = autorDAO.listar();
            model.setRowCount(0);
            for (Autor a : lista) {
                model.addRow(new Object[]{
                        a.getId(),
                        a.getNombre(),
                        a.getBiografia(),
                        a.getEstado() == 1 ? "Activo" : "Inactivo" // o usa 1/0 si prefieres
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtBiografia.setText("");
        cboEstado.setSelectedIndex(0);
        tblAutores.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Gestión de Autores");
            f.setContentPane(new AutorForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
