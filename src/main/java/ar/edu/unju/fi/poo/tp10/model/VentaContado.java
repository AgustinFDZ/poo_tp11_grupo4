package ar.edu.unju.fi.poo.tp10.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Venta de contado: se paga en el acto, aplica un descuento y no genera cuotas. */
@Entity
@Table(name = "ventas_contado")
@Getter @Setter @NoArgsConstructor
public class VentaContado extends Venta {

    @Column(name = "porcentaje_descuento", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "monto_descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    public VentaContado(BigDecimal porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    @Override
    public ModalidadPago getModalidadPago() {
        return ModalidadPago.CONTADO;
    }

    @Override
    protected BigDecimal calcularAjuste(BigDecimal subtotal) {
        this.descuento = subtotal.multiply(porcentajeDescuento).setScale(2, RoundingMode.HALF_UP);
        return descuento.negate();
    }
}
