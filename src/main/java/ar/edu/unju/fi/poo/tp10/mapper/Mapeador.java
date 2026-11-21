package ar.edu.unju.fi.poo.tp10.mapper;

import ar.edu.unju.fi.poo.tp10.dto.response.*;
import ar.edu.unju.fi.poo.tp10.model.*;
import org.hibernate.Hibernate;

import java.util.List;

/** Convierte entidades en DTOs de respuesta. Los controladores nunca ven entidades. */
public final class Mapeador {

    private Mapeador() {}

    public static ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getDniCuit(), c.getNombre(), c.getEmail(),
                c.getTelefono(), c.isHabilitado());
    }

    public static ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria(), p.getMarca(),
                p.getPrecio(), p.getStock(), p.getStockMinimo());
    }

    public static DetalleVentaResponse toResponse(DetalleVenta d) {
        return new DetalleVentaResponse(d.getProducto().getId(), d.getProducto().getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }

    public static CuotaResponse toResponse(Cuota c) {
        return new CuotaResponse(c.getId(), c.getVenta().getId(), c.getNumero(), c.getMonto(),
                c.getFechaVencimiento(), c.getEstado(), c.getFechaPago());
    }

    public static CobroCuotaResponse toCobroResponse(VentaFinanciada venta, Cuota cuota, boolean pagoFueraDeTermino) {
        return new CobroCuotaResponse(venta.getId(), toResponse(cuota), pagoFueraDeTermino, venta.getEstado(),
                (int) venta.cantidadCuotasPendientes(), venta.getSaldoPendiente());
    }

    /** Devuelve el DTO propio de cada modalidad de pago. */
    public static VentaResponse toResponse(Venta venta) {
        Venta v = (Venta) Hibernate.unproxy(venta); // por si llega como proxy lazy
        List<DetalleVentaResponse> detalles = v.getDetalles().stream().map(Mapeador::toResponse).toList();

        if (v instanceof VentaFinanciada f) {
            List<CuotaResponse> cuotas = f.getCuotas().stream().map(Mapeador::toResponse).toList();
            return new VentaFinanciadaResponse(f.getId(), f.getFecha(), f.getCliente().getId(),
                    f.getCliente().getNombre(), f.getModalidadPago(), f.getEstado(), f.getSubtotal(),
                    f.getTotal(), detalles, f.getTasaRecargo(), f.getRecargo(), f.getCantidadCuotas(), cuotas);
        }
        if (v instanceof VentaContado c) {
            return new VentaContadoResponse(c.getId(), c.getFecha(), c.getCliente().getId(),
                    c.getCliente().getNombre(), c.getModalidadPago(), c.getEstado(), c.getSubtotal(),
                    c.getTotal(), detalles, c.getPorcentajeDescuento(), c.getDescuento());
        }
        throw new IllegalStateException("Modalidad de venta no soportada: " + v.getClass().getSimpleName());
    }
}
