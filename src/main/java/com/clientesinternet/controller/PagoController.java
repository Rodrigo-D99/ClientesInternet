package com.clientesinternet.controller;

import com.clientesinternet.dto.req.NotaReq;
import com.clientesinternet.dto.req.PagoReq;
import com.clientesinternet.dto.resp.PagoHistorialResp;
import com.clientesinternet.dto.resp.PagoResp;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.clientesinternet.service.PagoService;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("pagos")
public class PagoController {
    @Autowired
    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping("/{clienteId}")
    public ResponseEntity<PagoResp> pagar(@PathVariable Long clienteId,
                                        @Valid @RequestBody PagoReq req) {
        return ResponseEntity.ok(service.registrarPago(clienteId, req));
    }
    @PatchMapping("/{id}/nota")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarNota(@PathVariable Long id,
                               @RequestBody NotaReq req) {
        service.actualizarNota(id, req.nota());
    }
    @GetMapping("/{clienteId}/historial")
    public ResponseEntity<List<PagoHistorialResp>> verHistorial(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.obtenerHistorial(clienteId));
    }
    @PutMapping("/{id}")
    public ResponseEntity<Void> editarPago(@PathVariable Long id, @RequestBody PagoReq req) {
        service.editarPago(id, req);
        return ResponseEntity.ok().build();
    }
   @GetMapping("/estadisticas/hoy")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasHoy() {
        // El controlador delega toda la responsabilidad al servicio
        Map<String, Object> estadisticas = service.obtenerEstadisticasHoy();
        return ResponseEntity.ok(estadisticas);
    }
}
