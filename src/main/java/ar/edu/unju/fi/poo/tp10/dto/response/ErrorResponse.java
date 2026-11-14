package ar.edu.unju.fi.poo.tp10.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Estructura JSON homogénea para todos los errores de la API. */
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String mensaje,
                            String ruta, List<String> detalles) {}
