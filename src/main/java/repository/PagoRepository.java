package repository;

import entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findFirstByClienteIdAndFechaPagoBetween(
            Long clienteId,
            LocalDate inicio,
            LocalDate fin
    );
    Optional<Pago> findTopByClienteIdOrderByFechaPagoDesc(Long clienteId);

}
