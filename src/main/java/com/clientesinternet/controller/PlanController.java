package com.clientesinternet.controller;

import com.clientesinternet.entity.PlanInternet;
import com.clientesinternet.service.PlanInternetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.MedioPago;
import java.util.List;
import com.clientesinternet.service.PagoService;
import com.clientesinternet.repository.ClienteRepository;

@RestController
@RequestMapping("api/planes")
public class PlanController {

    @Autowired
    private PlanInternetService planService;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private PagoService pagoService;

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
    // Endpoint para consultar la tarifa mensual estimada antes de procesar el pago
    @GetMapping("/estimar/{clienteId}")
    public ResponseEntity<BigDecimal> estimarMontoMensual(
            @PathVariable Long clienteId,
            @RequestParam(defaultValue = "EFECTIVO") MedioPago medioPago) {
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
                
        BigDecimal tarifaMensual = pagoService.calcularPrecioMensual(cliente, medioPago);
        return ResponseEntity.ok(tarifaMensual);
    }
}