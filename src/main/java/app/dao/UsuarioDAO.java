package app.dao;

import app.core.PasswordUtil;
import app.db.Conexion;
import app.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // ---- CREATE ----
    public int crearUsuario(String username, String plainPassword, String nombre, String rol) throws SQLException {
        return crearUsuario(username, plainPassword, nombre, rol, 1);
    }

    public int crearUsuario(String username, String plainPassword, String nombre, String rol, int estado) throws SQLException {
        String sql = "INSERT INTO usuario (username, password, nombre, rol, estado) VALUES (?,?,?,?,?)";
        String hash = PasswordUtil.hash(plainPassword);
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, nombre);
            ps.setString(4, rol);
            ps.setInt(5, estado);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // ---- READ ----
    public List<Usuario> listar(String filtroNombreOUser) throws SQLException {
        String base = "SELECT id, username, nombre, rol, estado FROM usuario";
        String where = (filtroNombreOUser == null || filtroNombreOUser.isBlank())
                ? "" : " WHERE username LIKE ? OR nombre LIKE ?";
        String order = " ORDER BY id DESC";
        String sql = base + where + order;

        List<Usuario> data = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (!where.isEmpty()) {
                String like = "%" + filtroNombreOUser.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new Usuario(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("nombre"),
                            rs.getString("rol"),
                            rs.getInt("estado")
                    ));
                }
            }
        }
        return data;
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, username, nombre, rol, estado FROM usuario WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Usuario(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("nombre"),
                        rs.getString("rol"),
                        rs.getInt("estado")
                );
            }
        }
    }

    public boolean existsUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM usuario WHERE username = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---- UPDATE ----
    public boolean actualizarPerfil(int id, String nombre, String rol, int estado) throws SQLException {
        String sql = "UPDATE usuario SET nombre=?, rol=?, estado=? WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, rol);
            ps.setInt(3, estado);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarPassword(int id, String plainPassword) throws SQLException {
        String sql = "UPDATE usuario SET password=? WHERE id=?";
        String hash = PasswordUtil.hash(plainPassword);
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---- DELETE lógico / Reactivar ----
    public boolean desactivar(int id) throws SQLException {
        String sql = "UPDATE usuario SET estado=0 WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean reactivar(int id) throws SQLException {
        String sql = "UPDATE usuario SET estado=1 WHERE id=?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---- LOGIN ----
    public Usuario validarLogin(String username, String plainPassword) throws SQLException {
        String sql = "SELECT id, username, password, nombre, rol, estado " +
                "FROM usuario WHERE username=? AND estado=1";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String hash = rs.getString("password");
                if (!PasswordUtil.verify(plainPassword, hash)) return null;
                return new Usuario(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("nombre"),
                        rs.getString("rol"),
                        rs.getInt("estado")
                );
            }
        }
    }
}