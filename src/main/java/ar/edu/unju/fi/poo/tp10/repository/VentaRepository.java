package ar.edu.unju.fi.poo.tp10.repository;

import ar.edu.unju.fi.poo.tp10.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);

    List<Venta> findByFechaBetweenOrderByFechaAscIdAsc(LocalDate desde, LocalDate hasta);

    @Query("SELECT v FROM Venta v WHERE TYPE(v) = VentaContado ORDER BY v.fecha DESC, v.id DESC")
    List<Venta> findContado();

    @Query("SELECT v FROM Venta v WHERE TYPE(v) = VentaFinanciada ORDER BY v.fecha DESC, v.id DESC")
    List<Venta> findFinanciadas();
}
