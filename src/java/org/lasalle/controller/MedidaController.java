/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.lasalle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.lasalle.connection.ConnectionMysql;
import org.lasalle.model.Medida;

/**
 *
 * @author santi
 */
public class MedidaController {
    public List<Medida> getMedida() throws SQLException{
    //declaramos la estructura de datos
    List<Medida> lista = new ArrayList<>();
    //delcaramos la sentencia a ejecutar
    String sql = "SELECT * FROM lectura_glucosa;";
    ConnectionMysql connMysql = new ConnectionMysql();

        // try-with-resources: cierra ResultSet, PreparedStatement y Connection
        // aunque ocurra una excepcion (evita fugas de conexiones en la nube).
        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(sql);
             ResultSet rs = pstm.executeQuery()) {

            while(rs.next()){
                Medida medida = new Medida();
                medida.setIdMedida(rs.getInt("id_lectura"));
                medida.setIdUsuario(rs.getInt("id_usuario"));
                medida.setGlucosa(rs.getInt("valor_mg_dl"));
                medida.setFechaHora(rs.getString("fecha_registro"));
                medida.setContexto(rs.getString("tipo_lectura"));
                medida.setNotas(rs.getString("notas"));
                lista.add(medida);
            }
        }
        return lista;
    }

    public int registrarMedida(Medida medida) throws SQLException {
        String sql = "INSERT INTO lectura_glucosa "
                + "(id_usuario, valor_mg_dl, tipo_lectura, notas, fecha_registro) "
                + "VALUES (?, ?, ?, ?, COALESCE(?, CURRENT_DATE))";

        ConnectionMysql connMysql = new ConnectionMysql();
        try (Connection conn = connMysql.open();
             PreparedStatement pstm = conn.prepareStatement(
                     sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            pstm.setInt(1, medida.getIdUsuario());
            pstm.setInt(2, medida.getGlucosa());
            pstm.setString(3, nvl(medida.getContexto()));
            pstm.setString(4, nvl(medida.getNotas()));

            if (medida.getFechaHora() == null || medida.getFechaHora().isBlank()) {
                pstm.setNull(5, java.sql.Types.DATE);
            } else {
                pstm.setDate(5, java.sql.Date.valueOf(medida.getFechaHora()));
            }

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
