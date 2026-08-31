package com.clientesinternet.repository;


import com.clientesinternet.entity.Cliente;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

       List<Cliente> findByNombreContainingIgnoreCaseOrDireccionContainingIgnoreCase(
            String nombre,
            String direccion,
            Sort sort
    );
}
