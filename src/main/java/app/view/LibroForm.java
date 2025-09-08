// src/main/java/app/view/LibroForm.java
package app.view;

import app.dao.AutorDAO;
import app.dao.LibroDAO;
import app.model.Autor;
import app.model.ComboItem;
import app.model.Libro;
import app.model.LibroConAutor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LibroForm {
    public JPanel panelPrincipal;
    private JTextField txtNombre;
    private JTextField txtAnio;
    private JComboBox<ComboItem> cboAutor;
    private JComboBox<String> cboEstado;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnCargar;
    private JTable tblLibros;

    private final AutorDAO autorDAO = new AutorDAO();
    private final LibroDAO libroDAO = new LibroDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Año", "Autor", "Estado"}, 0
    );

    private Integer selectedId = null;

    public LibroForm() {
        panelPrincipal.setPreferredSize(new Dimension(900, 600));
        tblLibros.setModel(model);
        tblLibros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // estado 1/0
        cboEstado.addItem("1 - Activo");
        cboEstado.addItem("0 - Inactivo");

        cargarAutoresEnCombo();

        btnGuardar.addActionListener(e -> onGuardar());
        btnActualizar.addActionListener(e -> onActualizar());
        btnCargar.addActionListener(e -> cargarTabla());

        // Al seleccionar fila, cargar a los campos
        tblLibros.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblLibros.getSelectedRow();
            if (row == -1) { selectedId = null; return; }

            selectedId = Integer.parseInt(model.getValueAt(row, 0).toString());
            txtNombre.setText(model.getValueAt(row, 1).toString());
            txtAnio.setText(model.getValueAt(row, 2).toString());

            // set autor en combo (buscar por label)
            String autorNombre = model.getValueAt(row, 3).toString();
            seleccionarAutorPorNombre(autorNombre);

            String estTxt = model.getValueAt(row, 4).toString();
            cboEstado.setSelectedIndex("Activo".equalsIgnoreCase(estTxt) ? 0 : 1);
        });
    }

    private void cargarAutoresEnCombo() {
        try {
            cboAutor.removeAllItems();
            List<Autor> autores = autorDAO.listar();
            for (Autor a : autores) {
                // usamos ComboItem(id, nombre)
                cboAutor.addItem(new ComboItem(a.getId(), a.getNombre()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void seleccionarAutorPorNombre(String nombre) {
        for (int i = 0; i < cboAutor.getItemCount(); i++) {
            ComboItem item = cboAutor.getItemAt(i);
            if (item.getLabel().equalsIgnoreCase(nombre)) {
                cboAutor.setSelectedIndex(i);
                return;
            }
        }
    }

    private void onGuardar() {
        String nombre = txtNombre.getText().trim();
        String anioStr = txtAnio.getText().trim();
        ComboItem autorItem = (ComboItem) cboAutor.getSelectedItem();
        int estado = (cboEstado.getSelectedIndex() == 0) ? 1 : 0;

        if (nombre.isEmpty() || anioStr.isEmpty() || autorItem == null) {
            txtNombre.requestFocus();
            return;
        }
        int anio;
        try { anio = Integer.parseInt(anioStr); }
        catch (NumberFormatException nfe) { txtAnio.requestFocus(); return; }

        try {
            Libro l = new Libro(nombre, anio, autorItem.getId(), estado);
            libroDAO.insertar(l);
            limpiarFormulario();
            cargarTabla();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onActualizar() {
        if (selectedId == null) return;

        String nombre = txtNombre.getText().trim();
        String anioStr = txtAnio.getText().trim();
        ComboItem autorItem = (ComboItem) cboAutor.getSelectedItem();
        int estado = (cboEstado.getSelectedIndex() == 0) ? 1 : 0;

        if (nombre.isEmpty() || anioStr.isEmpty() || autorItem == null) {
            txtNombre.requestFocus();
            return;
        }
        int anio;
        try { anio = Integer.parseInt(anioStr); }
        catch (NumberFormatException nfe) { txtAnio.requestFocus(); return; }

        try {
            Libro l = new Libro(selectedId, nombre, anio, autorItem.getId(), estado);
            boolean ok = libroDAO.actualizar(l);
            if (ok) {
                cargarTabla();
                seleccionarFilaPorId(selectedId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void cargarTabla() {
        try {
            List<LibroConAutor> lista = libroDAO.listarConAutor();
            model.setRowCount(0);
            for (LibroConAutor l : lista) {
                model.addRow(new Object[] {
                        l.getId(),
                        l.getNombre(),
                        l.getAnio(),
                        l.getAutorNombre(),
                        l.getEstado() == 1 ? "Activo" : "Inactivo"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtAnio.setText("");
        if (cboAutor.getItemCount() > 0) cboAutor.setSelectedIndex(0);
        cboEstado.setSelectedIndex(0);
        tblLibros.clearSelection();
        selectedId = null;
    }

    private void seleccionarFilaPorId(Integer id) {
        if (id == null) return;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object val = model.getValueAt(i, 0);
            if (val != null && Integer.parseInt(val.toString()) == id) {
                tblLibros.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Gestión de Libros");
            f.setContentPane(new LibroForm().panelPrincipal);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
