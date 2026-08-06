package com.clientesinternet.repository;

import com.clientesinternet.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findFirstByClienteIdAndFechaPagoBetween(
            Long clienteId,
            LocalDate inicio,
            LocalDate fin
    );
    Optional<Pago> findTopByClienteIdOrderByPeriodoPagadoDesc(Long clienteId);
    Optional<Pago> findByClienteId(Long clienteId);

}
