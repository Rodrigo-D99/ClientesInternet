package com.clientesinternet.controller;

import com.clientesinternet.dto.req.ClienteReq;
import com.clientesinternet.dto.req.NotaReq;
import com.clientesinternet.dto.req.PagoReq;
import com.clientesinternet.dto.resp.ClienteResp;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.clientesinternet.service.ClienteService;


@RestController
@RequestMapping("clientes")
public class ClienteController {
    @Autowired
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping("")
    public ResponseEntity<ClienteResp> crear(@Valid @RequestBody ClienteReq req) {
        return ResponseEntity.ok(service.save(req));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResp> modificarCliente(
            @PathVariable Long id,
            @RequestBody ClienteReq req) {
        ClienteResp actualizado = service.update(id, req);
        return ResponseEntity.ok(actualizado);
    }
    /**
     * Listado paginado con filtros
     */
    @GetMapping("")
    public ResponseEntity<Page<ClienteResp>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Boolean deudores,
            @RequestParam(required = false) String nombre
    ) {
        return ResponseEntity.ok(service.findPaged(page, size, deudores, nombre));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResp> getXId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?>  borrarCliente(@PathVariable Long id) {
         service.delete(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * Listado completo SOLO deudores (sin paginar)
     * Ideal para exportar
     */
    @GetMapping("/deudores")
    public ResponseEntity<?> listarDeudores(
            @RequestParam(required = false) String nombre
    ) {
        return ResponseEntity.ok(
                service.buscarClientesDeudores(true, nombre)
        );
    }
}
