package com.clientesinternet.controller;

import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.repository.ConfiguracionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/instalaciones")
    public ResponseEntity<List<Map<String, Object>>> obtenerInstalaciones() {
        List<Configuracion> configs = configRepo.findAll();
        List<Map<String, Object>> lista = configs.stream()
                .filter(c -> c.getClave() != null && c.getClave().startsWith("INSTALACION_"))
                .map(c -> {
                    String nombreTipo = c.getClave().replace("INSTALACION_", "");
                    return Map.of("nombre", (Object) nombreTipo, "precio", (Object) c.getValor());
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/instalaciones")
    public ResponseEntity<String> guardarInstalaciones(@RequestBody List<Map<String, Object>> instalaciones) {
        // 1. Limpiar instalaciones previas
        List<Configuracion> previas = configRepo.findAll().stream()
                .filter(c -> c.getClave() != null && c.getClave().startsWith("INSTALACION_"))
                .toList();
        configRepo.deleteAll(previas);

        // 2. Guardar las nuevas convertidas en Mayúsculas
        for (Map<String, Object> inst : instalaciones) {
            String nombre = String.valueOf(inst.get("nombre")).trim().toUpperCase();
            Double precio = Double.parseDouble(String.valueOf(inst.get("precio")));
            
            Configuracion config = new Configuracion("INSTALACION_" + nombre, precio);
            configRepo.save(config);
        }

        return ResponseEntity.ok("Instalaciones actualizadas correctamente");
    }
}