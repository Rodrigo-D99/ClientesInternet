package controller;

import dto.req.PagoReq;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    @Autowired
    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping("/{clienteId}")
    public void pagar(@PathVariable Long clienteId,
                      @Valid @RequestBody PagoReq req) {
        service.registrarPago(clienteId, req);
    }
}
