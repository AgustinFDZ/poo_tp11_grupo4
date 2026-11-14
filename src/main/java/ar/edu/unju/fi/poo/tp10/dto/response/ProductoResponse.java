package ar.edu.unju.fi.poo.tp10.dto.response;

import java.math.BigDecimal;

public record ProductoResponse(Long id, String nombre, String categoria, String marca,
                               BigDecimal precio, int stock, int stockMinimo) {}
