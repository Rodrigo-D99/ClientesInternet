package com.clientesinternet.controller;

import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.repository.ConfiguracionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // Importante agregar esto

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

    // Endpoint para guardar el precio de TV por Cable
    @PostMapping("/cabletv")
    public ResponseEntity<Configuracion> guardarPrecioCableTv(@RequestParam Double precio) {
        Configuracion config = new Configuracion("PRECIO_CABLE_TV", precio);
        return ResponseEntity.ok(configRepo.save(config));
    }
    
    @GetMapping("/cabletv")
    public ResponseEntity<Configuracion> obtenerPrecioCableTv() {
        return ResponseEntity.ok(configRepo.findById("PRECIO_CABLE_TV")
            .orElse(new Configuracion("PRECIO_CABLE_TV", 0.0)));
    }

    // ==========================================
    // NUEVOS ENDPOINTS PARA EL CORREO
    // ==========================================
    @PostMapping("/email")
    public ResponseEntity<String> guardarEmail(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        if (email != null && !email.trim().isEmpty()) {
            configRepo.save(new Configuracion("EMAIL_ADMIN", email.trim()));
            System.out.println("LOG CONFIG: Email guardado -> " + email.trim());
        }
        
        if (password != null && !password.trim().isEmpty()) {
            // Elimina todos los espacios en blanco de las 16 letras
            String passLimpia = password.replaceAll("\\s+", "");
            configRepo.save(new Configuracion("PASSWORD_ADMIN", passLimpia));
            System.out.println("LOG CONFIG: Password guardada. Longitud -> " + passLimpia.length());
        }

        return ResponseEntity.ok("Guardado correctamente");
    }
    @GetMapping("/email")
    public ResponseEntity<String> obtenerEmail() {
        String email = configRepo.findById("EMAIL_ADMIN")
                .map(Configuracion::getValorString)
                .orElse("");
        return ResponseEntity.ok(email);
    }
}