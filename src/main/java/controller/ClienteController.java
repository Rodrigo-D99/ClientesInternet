package controller;

import dto.req.ClienteReq;
import dto.resp.ClienteResp;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import service.ClienteService;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    @Autowired
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    public ClienteResp crear(@Valid @RequestBody ClienteReq req) {
        return service.save(req);
    }

    @GetMapping
    public Page<ClienteResp> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Boolean deudores,
            @RequestParam(required = false) String nombre
    ) {
        return service.findPaged(page, size, deudores, nombre);
    }


}
