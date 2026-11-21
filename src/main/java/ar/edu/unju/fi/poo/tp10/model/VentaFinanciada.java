package ar.edu.unju.fi.poo.tp10.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Venta financiada: aplica un recargo por financiación y se paga mediante un plan de cuotas. */
@Entity
@Table(name = "ventas_financiadas")
@Getter @Setter @NoArgsConstructor
public class VentaFinanciada extends Venta {

    @Column(name = "tasa_recargo", nullable = false, precision = 5, scale = 4)
    private BigDecimal tasaRecargo = BigDecimal.ZERO;

    @Column(name = "monto_recargo", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargo = BigDecimal.ZERO;

    @Column(name = "cantidad_cuotas", nullable = false)
    private Integer cantidadCuotas;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<Cuota> cuotas = new ArrayList<>();

    public VentaFinanciada(BigDecimal tasaRecargo, Integer cantidadCuotas) {
        this.tasaRecargo = tasaRecargo;
        this.cantidadCuotas = cantidadCuotas;
    }

    @Override
    public ModalidadPago getModalidadPago() {
        return ModalidadPago.FINANCIADA;
    }

    @Override
    protected BigDecimal calcularAjuste(BigDecimal subtotal) {
        this.recargo = subtotal.multiply(tasaRecargo).setScale(2, RoundingMode.HALF_UP);
        return recargo;
    }

    public void agregarCuota(Cuota cuota) {
        cuota.setVenta(this);
        cuotas.add(cuota);
    }

    /** Primera cuota (por número) que aún no se pagó: la única que puede cobrarse, sin saltear ni adelantar cuotas. */
    public Optional<Cuota> proximaCuotaPendiente() {
        return cuotas.stream()
                .filter(c -> !c.isPagada())
                .min(Comparator.comparingInt(Cuota::getNumero));
    }

    public boolean tieneCuotasPendientes() {
        return cuotas.stream().anyMatch(c -> !c.isPagada());
    }

    public long cantidadCuotasPendientes() {
        return cuotas.stream().filter(c -> !c.isPagada()).count();
    }

    /** Pasa a VENCIDA todas las cuotas adeudadas cuyo vencimiento ya pasó. */
    public void sincronizarVencimientos(LocalDate hoy) {
        cuotas.forEach(c -> c.sincronizarEstado(hoy));
    }

    public BigDecimal getSaldoPendiente() {
        return cuotas.stream()
                .filter(c -> !c.isPagada())
                .map(Cuota::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
