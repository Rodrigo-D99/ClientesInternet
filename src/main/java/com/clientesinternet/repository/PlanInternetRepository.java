package com.clientesinternet.repository;

import com.clientesinternet.entity.PlanInternet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface PlanInternetRepository extends JpaRepository<PlanInternet, Long> {
    void deleteAllByIdNotIn(List<Long> ids);
    Optional<PlanInternet> findByCantidadMB(Integer cantidadMB);
}