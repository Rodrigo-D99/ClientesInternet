package com.clientesinternet.repository;


import com.clientesinternet.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

        // Consulta que permite filtrar por nombre y/o deudores, y soporta Sort nativo
        @Query("SELECT c FROM Cliente c WHERE " +
                "(:nombre IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
                "(:soloDeudores IS NULL OR " +
                "  (:soloDeudores = true AND c.mesesPagados < 1) OR " + 
                "  (:soloDeudores = false AND c.mesesPagados >= 1))")
        Page<Cliente> findFiltered(
                @Param("nombre") String nombre, 
                @Param("soloDeudores") Boolean soloDeudores, 
                Pageable pageable
        );

        Page<Cliente> findAll(Pageable pageable);

        Page<Cliente> findByNombreContainingIgnoreCase(
                String nombre,
                Pageable pageable
        );
        List<Cliente> findByNombreContainingIgnoreCase(
                String nombre,
                Sort pageable
        );
}
