package ar.edu.unju.fi.poo.tp10.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // VARCHAR (no ENUM nativo de MySQL) para poder sumar estados sin alterar la columna
    @Column(nullable = false, length = 20)
    private EstadoCuota estado = EstadoCuota.ADEUDADA;

    private LocalDate fechaPago;

    public boolean isPagada() {
        return estado == EstadoCuota.PAGADA;
    }

    /** Una cuota impaga está vencida cuando la fecha dada es posterior a su vencimiento (el día de vencimiento aún se puede pagar). */
    public boolean estaVencidaAl(LocalDate fecha) {
        return !isPagada() && fecha.isAfter(fechaVencimiento);
    }

    /** Si la cuota sigue adeudada y ya pasó su vencimiento, pasa automáticamente a VENCIDA. */
    public void sincronizarEstado(LocalDate hoy) {
        if (estado == EstadoCuota.ADEUDADA && hoy.isAfter(fechaVencimiento)) {
            estado = EstadoCuota.VENCIDA;
        }
    }

    /** Registra el pago de la cuota por su monto original. */
    public void registrarPago(LocalDate hoy) {
        if (isPagada()) {
            throw new IllegalStateException("La cuota " + id + " ya fue pagada");
        }
        this.estado = EstadoCuota.PAGADA;
        this.fechaPago = hoy;
    }
}
