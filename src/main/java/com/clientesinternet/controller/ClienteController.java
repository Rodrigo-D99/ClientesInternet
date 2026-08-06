package com.clientesinternet.controller;

import com.clientesinternet.dto.req.ClienteReq;
import com.clientesinternet.dto.resp.ClienteResp;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    
    @PatchMapping("/{id}")
    public ResponseEntity<ClienteResp> actualizarParcial(
            @PathVariable Long id,
            @RequestBody ClienteReq req) {
        ClienteResp actualizado = service.update(id, req);
        return ResponseEntity.ok(actualizado);
    }
    /**
     * Listado paginado con filtros
     */
    @GetMapping
    public Page<ClienteResp> listar(
            @RequestParam (defaultValue = "0") int page,
            @RequestParam (defaultValue = "5") int size,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean deudores,
            @RequestParam(defaultValue = "nombre") String sort,
            @RequestParam(defaultValue = "asc") String dir
    ) {
        return service.findPaged(
                page,
                size,
                deudores,
                nombre,
                sort,
                dir
        );
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
    @DeleteMapping("/all")
    public ResponseEntity<?> borrarTodo() {
        service.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
