package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.response.CobroCuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.CuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.OperacionNoPermitidaException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.mapper.Mapeador;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.Cuota;
import ar.edu.unju.fi.poo.tp10.model.EstadoCuota;
import ar.edu.unju.fi.poo.tp10.model.Venta;
import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;
import ar.edu.unju.fi.poo.tp10.repository.CuotaRepository;
import ar.edu.unju.fi.poo.tp10.repository.VentaRepository;
import ar.edu.unju.fi.poo.tp10.service.IClienteValidador;
import ar.edu.unju.fi.poo.tp10.service.ICuotaService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CuotaServiceImpl implements ICuotaService {

    private final CuotaRepository cuotaRepository;
    private final VentaRepository ventaRepository;
    private final IClienteValidador clienteValidador;
    private final Clock clock;

    /**
     * No es de solo lectura a propósito: al consultar, las cuotas adeudadas cuyo vencimiento ya pasó
     * se persisten como VENCIDA.
     */
    @Override
    public EstadoCuentaResponse estadoDeCuenta(Long clienteId) {
        Cliente cliente = clienteValidador.obtenerExistente(clienteId);
        LocalDate hoy = LocalDate.now(clock);

        List<Cuota> pendientes = cuotaRepository.findPendientesByCliente(clienteId);
        pendientes.forEach(c -> c.sincronizarEstado(hoy));

        List<VentaResponse> ventas = ventaRepository.findByClienteIdOrderByFechaDescIdDesc(clienteId)
                .stream().map(Mapeador::toResponse).toList();
        List<CuotaResponse> cuotas = pendientes.stream().map(Mapeador::toResponse).toList();

        List<CuotaResponse> vencidas = cuotas.stream().filter(c -> c.estado() == EstadoCuota.VENCIDA).toList();
        return new EstadoCuentaResponse(Mapeador.toResponse(cliente), ventas,
                cuotas.size(), vencidas.size(), sumar(cuotas), sumar(vencidas), cuotas);
    }

    @Override
    public CobroCuotaResponse cobrarCuota(Long ventaId) {
        // bloquea la venta: dos cobros simultáneos no pueden pagar la misma cuota
        Venta encontrada = ventaRepository.buscarConBloqueo(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));

        if (!(Hibernate.unproxy(encontrada) instanceof VentaFinanciada venta)) {
            throw new OperacionNoPermitidaException("La venta " + ventaId + " es de contado y no tiene cuotas para cobrar");
        }
        if (venta.estaAnulada()) {
            throw new OperacionNoPermitidaException("La venta " + ventaId + " está anulada");
        }
        if (venta.estaSaldada()) {
            throw new OperacionNoPermitidaException("La venta " + ventaId + " ya está saldada");
        }

        LocalDate hoy = LocalDate.now(clock);
        venta.sincronizarVencimientos(hoy);

        // siempre la primera cuota impaga: no se puede saltear ni adelantar vencimientos futuros
        Cuota cuota = venta.proximaCuotaPendiente()
                .orElseThrow(() -> new OperacionNoPermitidaException("La venta " + ventaId + " no tiene cuotas pendientes"));

        boolean fueraDeTermino = cuota.getEstado() == EstadoCuota.VENCIDA;
        cuota.registrarPago(hoy);

        if (!venta.tieneCuotasPendientes()) {
            venta.saldar();
        }
        ventaRepository.save(venta);
        return Mapeador.toCobroResponse(venta, cuota, fueraDeTermino);
    }

    private static BigDecimal sumar(List<CuotaResponse> cuotas) {
        return cuotas.stream().map(CuotaResponse::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
