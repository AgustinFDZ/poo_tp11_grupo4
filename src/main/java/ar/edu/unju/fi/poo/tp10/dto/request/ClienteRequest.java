package ar.edu.unju.fi.poo.tp10.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClienteRequest(
        @NotBlank(message = "El DNI/CUIT es obligatorio")
        @Pattern(regexp = "^(\\d{7,8}|\\d{11})$", message = "El DNI debe tener 7 u 8 dígitos o el CUIT 11 dígitos (sin guiones)")
        String dniCuit,

        @NotBlank(message = "El nombre o razón social es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        String telefono,

        Boolean habilitado
) {}
