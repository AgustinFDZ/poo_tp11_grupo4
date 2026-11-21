package ar.edu.unju.fi.poo.tp10.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Venta abstracta. Cada modalidad de pago (contado / financiada) es una subclase con sus
 * propias reglas: cómo ajusta el subtotal (descuento o recargo) y qué datos propios guarda.
 */
@Entity
@Table(name = "ventas")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter @NoArgsConstructor
public abstract class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado = EstadoVenta.ACTIVA;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    /** Modalidad de pago de esta venta (cada subclase responde la suya). */
    public abstract ModalidadPago getModalidadPago();

    /**
     * Calcula el ajuste que la modalidad aplica sobre el subtotal:
     * negativo para un descuento, positivo para un recargo. Cada subclase guarda además su detalle.
     */
    protected abstract BigDecimal calcularAjuste(BigDecimal subtotal);

    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        detalles.add(detalle);
    }

    /** Template method: subtotal de los detalles + ajuste propio de la modalidad. */
    public void recalcular() {
        this.subtotal = detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.add(calcularAjuste(this.subtotal));
    }

    public boolean estaAnulada() {
        return estado == EstadoVenta.ANULADA;
    }

    public boolean estaSaldada() {
        return estado == EstadoVenta.SALDADA;
    }

    public void saldar() {
        this.estado = EstadoVenta.SALDADA;
    }

    public void anular() {
        this.estado = EstadoVenta.ANULADA;
    }
}
