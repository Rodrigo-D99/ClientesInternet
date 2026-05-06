package com.clientesinternet.service;

import com.clientesinternet.entity.PlanInternet;
import com.clientesinternet.repository.PlanInternetRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanInternetService {

    @Autowired
    private PlanInternetRepository planRepository;

    @Transactional
    public PlanInternet save(PlanInternet plan) {
        return planRepository.save(plan);
    }

    @Transactional
    public void sincronizarPlanes(List<PlanInternet> planesNuevos) {
        // 1. Guardamos todo primero. Si un plan no tiene ID, se le creará uno nuevo.
        List<PlanInternet> planesGuardados = planRepository.saveAll(planesNuevos);

        // 2. Extraemos los IDs de los que acaban de guardarse/actualizarse
        List<Long> idsGuardados = planesGuardados.stream()
                .map(PlanInternet::getId)
                .collect(Collectors.toList());

        // 3. Borramos los que no estén en esa lista final
        if (!idsGuardados.isEmpty()) {
            planRepository.deleteAllByIdNotIn(idsGuardados);
        } else {
            planRepository.deleteAll();
        }
    }

    public List<PlanInternet> listarTodos() {
        return planRepository.findAll();
    }
}