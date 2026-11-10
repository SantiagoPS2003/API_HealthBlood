/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.lasalle.controller;

import com.mysql.cj.jdbc.PreparedStatementWrapper;
import java.util.ArrayList;
import java.util.List;
import org.lasalle.connection.ConnectionMysql;
import org.lasalle.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
/**
 *
 * @author santi
 */
public class UsuarioController {
    public List<Usuario> getUsuarios() throws SQLException{
    //declaramos la estructura de datos
    List<Usuario> lista = new ArrayList<>();
    //delcaramos la sentencia a ejecutar
    String sql = "SELECT * FROM users;";
    ConnectionMysql connMysql = new ConnectionMysql();

        // try-with-resources: cierra ResultSet, PreparedStatement y Connection
        // aunque ocurra una excepcion (evita fugas de conexiones en la nube).
        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {

            while(rs.next()){
                Usuario user = new Usuario();
                user.setIdUsuario(rs.getInt("id_usuario"));
                user.setNombre(rs.getString("nombre"));
                user.setPrimerApellido(rs.getString("primer_apellido"));
                user.setSegundoApellido(rs.getString("segundo_apellido"));
                user.setEmail(rs.getString("email"));
                lista.add(user);
            }
        }
        return lista;
    }

    public int registrarUsuario(Usuario user) throws SQLException {
        String sql = "INSERT INTO users "
                + "(nombre, primer_apellido, segundo_apellido, fecha_nacimiento, "
                + " tipo_diabetes, fecha_registro, email) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)";

        ConnectionMysql connMysql = new ConnectionMysql();
        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(
                     sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            pstm.setString(1, user.getNombre());
            pstm.setString(2, user.getPrimerApellido());
            pstm.setString(3, nvl(user.getSegundoApellido()));

            if (user.getFechaNacimiento() == null || user.getFechaNacimiento().isBlank()) {
                pstm.setNull(4, java.sql.Types.DATE);
            } else {
                pstm.setDate(4, java.sql.Date.valueOf(user.getFechaNacimiento()));
            }

            pstm.setString(5, nvl(user.getTipoDiabetes()));
            pstm.setString(6, nvl(user.getEmail()));

            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private String nvl(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
