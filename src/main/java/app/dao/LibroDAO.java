// src/main/java/app/dao/LibroDAO.java
package app.dao;

import app.db.Conexion;
import app.model.Libro;
import app.model.LibroConAutor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibroDAO {

    public int insertar(Libro l) throws SQLException {
        String sql = "INSERT INTO libro (nombre, anio, idAutor, estado) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, l.getNombre());
            ps.setInt(2, l.getAnio());
            ps.setInt(3, l.getIdAutor());
            ps.setInt(4, l.getEstado());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    l.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public boolean actualizar(Libro l) throws SQLException {
        String sql = "UPDATE libro SET nombre=?, anio=?, idAutor=?, estado=? WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, l.getNombre());
            ps.setInt(2, l.getAnio());
            ps.setInt(3, l.getIdAutor());
            ps.setInt(4, l.getEstado());
            ps.setInt(5, l.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM libro WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Libro buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre, anio, idAutor, estado FROM libro WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Libro(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getInt("anio"),
                            rs.getInt("idAutor"),
                            rs.getInt("estado")
                    );
                }
            }
        }
        return null;
    }

    // Lista con JOIN para mostrar el nombre del autor en la tabla
    public List<LibroConAutor> listarConAutor() throws SQLException {
        String sql = """
                SELECT l.id, l.nombre, l.anio, a.nombre AS autorNombre, l.estado
                FROM libro l
                JOIN autor a ON a.id = l.idAutor
                ORDER BY l.id DESC
                """;
        List<LibroConAutor> data = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                data.add(new LibroConAutor(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getInt("anio"),
                        rs.getString("autorNombre"),
                        rs.getInt("estado")
                ));
            }
        }
        return data;
    }
}
