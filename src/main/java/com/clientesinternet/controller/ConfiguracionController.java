package com.clientesinternet.controller;

import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.repository.ConfiguracionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/configuracion")
public class ConfiguracionController {

    private final ConfiguracionRepository configRepo;

    public ConfiguracionController(ConfiguracionRepository configRepo) {
        this.configRepo = configRepo;
    }

    // Endpoint para guardar el precio de la Fibra TV
    @PostMapping("/fibratv")
    public ResponseEntity<Configuracion> guardarPrecioFibraTv(@RequestParam Double precio) {
        Configuracion config = new Configuracion("PRECIO_FIBRA_TV", precio);
        return ResponseEntity.ok(configRepo.save(config));
    }
    @GetMapping("/fibratv")
    public ResponseEntity<Configuracion> obtenerPrecioFibraTv() {
        return ResponseEntity.ok(configRepo.findById("PRECIO_FIBRA_TV")
            .orElse(new Configuracion("PRECIO_FIBRA_TV", 0.0)));
    }
}