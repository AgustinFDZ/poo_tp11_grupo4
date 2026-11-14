package ar.edu.unju.fi.poo.tp10.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuotas")
@Getter @Setter @NoArgsConstructor
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private VentaFinanciada venta;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    private boolean pagada = false;

    private LocalDate fechaPago;
}
