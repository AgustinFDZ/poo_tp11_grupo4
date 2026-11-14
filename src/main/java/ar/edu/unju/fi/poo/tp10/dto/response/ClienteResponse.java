package ar.edu.unju.fi.poo.tp10.dto.response;

public record ClienteResponse(Long id, String dniCuit, String nombre, String email, String telefono, boolean habilitado) {}
