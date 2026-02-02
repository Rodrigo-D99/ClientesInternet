package com.clientesinternet.controller;

import com.clientesinternet.dto.req.NotaReq;
import com.clientesinternet.dto.req.PagoReq;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.clientesinternet.service.PagoService;

@RestController
@RequestMapping("pagos")
public class PagoController {
    @Autowired
    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping("/{clienteId}")
    public void pagar(@PathVariable Long clienteId,
                      @Valid @RequestBody PagoReq req) {
        ResponseEntity.ok(service.registrarPago(clienteId, req));
    }
    @PatchMapping("/{id}/nota")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarNota(@PathVariable Long id,
                               @RequestBody NotaReq req) {
        service.actualizarNota(id, req.nota());
    }

}
