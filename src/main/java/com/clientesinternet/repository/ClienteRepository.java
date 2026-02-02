package com.clientesinternet.repository;


import com.clientesinternet.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("ProdRepository")
public interface ClienteRepository  extends JpaRepository<Cliente, Long> {
    Page<Cliente> findAll(Pageable pageable);

    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
