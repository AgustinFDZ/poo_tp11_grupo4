package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.response.CuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.OperacionNoPermitidaException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.mapper.Mapeador;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.Cuota;
import ar.edu.unju.fi.poo.tp10.repository.CuotaRepository;
import ar.edu.unju.fi.poo.tp10.service.IClienteValidador;
import ar.edu.unju.fi.poo.tp10.service.ICuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CuotaServiceImpl implements ICuotaService {

    private final CuotaRepository cuotaRepository;
    private final IClienteValidador clienteValidador;

    @Override
    @Transactional(readOnly = true)
    public EstadoCuentaResponse estadoDeCuenta(Long clienteId) {
        Cliente cliente = clienteValidador.obtenerExistente(clienteId);
        List<CuotaResponse> pendientes = cuotaRepository.findPendientesByCliente(clienteId)
                .stream().map(Mapeador::toResponse).toList();
        BigDecimal total = pendientes.stream().map(CuotaResponse::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EstadoCuentaResponse(cliente.getId(), cliente.getNombre(), pendientes.size(), total, pendientes);
    }

    @Override
    public CuotaResponse pagarCuota(Long cuotaId) {
        Cuota c = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuota", cuotaId));
        if (c.getVenta().estaAnulada()) {
            throw new OperacionNoPermitidaException("La venta de la cuota " + cuotaId + " está anulada");
        }
        if (c.isPagada()) {
            throw new OperacionNoPermitidaException("La cuota " + cuotaId + " ya fue pagada");
        }
        c.setPagada(true);
        c.setFechaPago(LocalDate.now());
        return Mapeador.toResponse(cuotaRepository.save(c));
    }
}
