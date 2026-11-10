/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.lasalle.rest;

import com.google.gson.Gson;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lasalle.controller.UsuarioController;
import org.lasalle.model.Usuario;
import org.lasalle.controller.MedidaController;
import org.lasalle.model.Medida;

/**
 *
 * @author santi
 */
@Path("glucosa")
public class RestGlucosa {
    
    
    @Path("saludar")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response saludar(){
        String saludo = """
                        {"Saludo":"Hola desde java"}
                        """;
        return Response.ok(saludo).build();
    }
    
    @Path("saludarCumpleanios")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response saludarCumpleanios(
            @QueryParam("nombre") String nombre
    ){
        String out = """
                     {"response":"Feliz cumple %s"}
                     """;
        out = String.format(out,nombre);
        return Response.ok(out).build();
    }
    
    @Path("getAllUsuarios")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsuarios(){
        UsuarioController uc = new UsuarioController();
        List<Usuario> lista = new ArrayList<>();
        Gson gson = new Gson();
        String out = "";
        try {
            lista = uc.getUsuarios();
            out = gson.toJson(lista);
        } catch (Exception e) {
            e.printStackTrace();
            out ="{\"error\": \"" + e.getMessage() + "\"}";

        }
        return Response.ok(out).build();
    }    
    
    @Path("getAllMedidas")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMedidas(){
        MedidaController uc = new MedidaController();
        List<Medida> lista = new ArrayList<>();
        Gson gson = new Gson();
        String out = "";
        try {
            lista = uc.getMedida();
            out = gson.toJson(lista);
        } catch (Exception e) {
            e.printStackTrace();
            out ="{\"error\": \"" + e.getMessage() + "\"}";

        }
        return Response.ok(out).build();
    }

    @Path("registrarUsuario")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarUsuario(
            @FormParam("nombre") String nombre,
            @FormParam("primerApellido") String primerApellido,
            @FormParam("segundoApellido") String segundoApellido,
            @FormParam("fechaNacimiento") String fechaNacimiento,
            @FormParam("tipoDiabetes") String tipoDiabetes,
            @FormParam("email") String email) {

        if (nombre == null || nombre.isBlank()
                || primerApellido == null || primerApellido.isBlank()) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "nombre y primerApellido son obligatorios");
        }
        if (nombre.length() > 45 || primerApellido.length() > 45
                || (segundoApellido != null && segundoApellido.length() > 45)
                || (tipoDiabetes != null && tipoDiabetes.length() > 30)
                || (email != null && email.length() > 50)) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "uno de los campos excede la longitud permitida");
        }

        Usuario user = new Usuario();
        user.setNombre(nombre.trim());
        user.setPrimerApellido(primerApellido.trim());
        user.setSegundoApellido(segundoApellido);
        user.setFechaNacimiento(fechaNacimiento);
        user.setTipoDiabetes(tipoDiabetes);
        user.setEmail(email);

        try {
            int id = new UsuarioController().registrarUsuario(user);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"status\":\"ok\",\"idUsuario\":" + id + "}")
                    .build();
        } catch (IllegalArgumentException e) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "fechaNacimiento debe tener formato yyyy-MM-dd");
        } catch (SQLException e) {
            return jsonError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Path("registrarMedida")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarMedida(
            @FormParam("idUsuario") Integer idUsuario,
            @FormParam("valorMgDl") Integer valorMgDl,
            @FormParam("tipoLectura") String tipoLectura,
            @FormParam("notas") String notas,
            @FormParam("fechaRegistro") String fechaRegistro) {

        if (idUsuario == null || idUsuario <= 0) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "idUsuario es obligatorio y debe ser mayor a 0");
        }
        if (valorMgDl == null || valorMgDl <= 0 || valorMgDl > 65535) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "valorMgDl debe estar entre 1 y 65535");
        }
        if (tipoLectura != null && tipoLectura.length() > 45) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "tipoLectura no puede superar 45 caracteres");
        }

        Medida medida = new Medida();
        medida.setIdUsuario(idUsuario);
        medida.setGlucosa(valorMgDl);
        medida.setContexto(tipoLectura);
        medida.setNotas(notas);
        medida.setFechaHora(fechaRegistro);

        try {
            int id = new MedidaController().registrarMedida(medida);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"status\":\"ok\",\"idLectura\":" + id + "}")
                    .build();
        } catch (IllegalArgumentException e) {
            return jsonError(Response.Status.BAD_REQUEST,
                    "fechaRegistro debe tener formato yyyy-MM-dd");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1452) {
                return jsonError(Response.Status.BAD_REQUEST,
                        "el idUsuario no existe");
            }
            return jsonError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Response jsonError(Response.Status status, String mensaje) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "error");
        body.put("mensaje", mensaje == null ? "Error desconocido" : mensaje);
        return Response.status(status).entity(new Gson().toJson(body)).build();
    }

}
