package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.OperacionNoPermitidaException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.mapper.Mapeador;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.model.Venta;
import ar.edu.unju.fi.poo.tp10.repository.VentaRepository;
import ar.edu.unju.fi.poo.tp10.service.IClienteValidador;
import ar.edu.unju.fi.poo.tp10.service.IPoliticaPago;
import ar.edu.unju.fi.poo.tp10.service.IStockService;
import ar.edu.unju.fi.poo.tp10.service.IVentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestador de ventas. No contiene reglas de pago ni de stock: las delega en
 * {@link IClienteValidador}, {@link IStockService} y la {@link IPoliticaPago} de la modalidad elegida.
 */
@Service
@Transactional
public class VentaServiceImpl implements IVentaService {

    private final VentaRepository ventaRepository;
    private final IClienteValidador clienteValidador;
    private final IStockService stockService;
    private final Map<ModalidadPago, IPoliticaPago> politicas = new EnumMap<>(ModalidadPago.class);

    public VentaServiceImpl(VentaRepository ventaRepository,
                            IClienteValidador clienteValidador,
                            IStockService stockService,
                            List<IPoliticaPago> politicas) {
        this.ventaRepository = ventaRepository;
        this.clienteValidador = clienteValidador;
        this.stockService = stockService;
        politicas.forEach(p -> this.politicas.put(p.modalidad(), p));
    }

    /**
     * Registra una venta: valida el pedido según la modalidad, valida al cliente, reserva el stock
     * y delega en la política de pago la construcción de la venta (descuento / recargo / cuotas).
     * Ante cualquier excepción la transacción se revierte (el stock no se modifica).
     */
    @Override
    public VentaResponse registrar(VentaRequest request) {
        IPoliticaPago politica = politicaPara(request.modalidadPago());
        politica.validar(request);

        Cliente cliente = clienteValidador.obtenerHabilitado(request.clienteId());
        List<DetalleVenta> detalles = stockService.reservar(request.items());

        Venta venta = politica.crearVenta(cliente, detalles, request);
        return Mapeador.toResponse(ventaRepository.save(venta));
    }

    /** Anula la venta y repone el stock. */
    @Override
    public VentaResponse anular(Long id) {
        Venta venta = buscarEntidad(id);
        if (venta.estaAnulada()) {
            throw new OperacionNoPermitidaException("La venta " + id + " ya se encuentra anulada");
        }
        stockService.reponer(venta);
        venta.anular();
        return Mapeador.toResponse(ventaRepository.save(venta));
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        return Mapeador.toResponse(buscarEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventaRepository.findAll().stream().map(Mapeador::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> historialPorCliente(Long clienteId) {
        clienteValidador.obtenerExistente(clienteId);
        return ventaRepository.findByClienteIdOrderByFechaDescIdDesc(clienteId)
                .stream().map(Mapeador::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> buscarPorPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        return ventaRepository.findByFechaBetweenOrderByFechaAscIdAsc(desde, hasta)
                .stream().map(Mapeador::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> buscarPorModalidad(ModalidadPago modalidad) {
        List<Venta> ventas = switch (modalidad) {
            case CONTADO -> ventaRepository.findContado();
            case FINANCIADA -> ventaRepository.findFinanciadas();
        };
        return ventas.stream().map(Mapeador::toResponse).toList();
    }

    // ---- internos ----
    private IPoliticaPago politicaPara(ModalidadPago modalidad) {
        IPoliticaPago politica = politicas.get(modalidad);
        if (politica == null) {
            throw new SolicitudInvalidaException("Modalidad de pago no soportada: " + modalidad);
        }
        return politica;
    }

    private Venta buscarEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }
}
