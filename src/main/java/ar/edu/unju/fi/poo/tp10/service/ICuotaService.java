package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.response.CuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;

public interface ICuotaService {
    EstadoCuentaResponse estadoDeCuenta(Long clienteId);
    CuotaResponse pagarCuota(Long cuotaId);
}
