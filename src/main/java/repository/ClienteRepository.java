package repository;


import entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("ProdRepository")
public interface ClienteRepository  extends JpaRepository<Cliente, Long> {
    Page<Cliente> findAll(Pageable pageable);

}
