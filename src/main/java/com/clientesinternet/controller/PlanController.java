package com.clientesinternet.controller;

import com.clientesinternet.entity.PlanInternet;
import com.clientesinternet.service.PlanInternetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/planes")
public class PlanController {

    @Autowired
    private PlanInternetService planService;

    // Guardar o Actualizar un precio (esto es lo que usará tu cliente cada 3 meses)
    @PostMapping
    public ResponseEntity<PlanInternet> guardarPlan(@RequestBody PlanInternet plan) {
        return ResponseEntity.ok(planService.save(plan));
    
    }

    @GetMapping
    public List<PlanInternet> listar() {
        return planService.listarTodos();
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar(@RequestBody List<PlanInternet> planes) {
        planService.sincronizarPlanes(planes);
        return ResponseEntity.ok().build();
    }
}