package ar.edu.unju.fi.poo.tp10.repository;

import ar.edu.unju.fi.poo.tp10.model.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    @Query("""
            SELECT c FROM Cuota c
            WHERE c.venta.cliente.id = :clienteId
              AND c.pagada = false
              AND c.venta.estado = ar.edu.unju.fi.poo.tp10.model.EstadoVenta.ACTIVA
            ORDER BY c.fechaVencimiento ASC, c.numero ASC
            """)
    List<Cuota> findPendientesByCliente(@Param("clienteId") Long clienteId);
}
