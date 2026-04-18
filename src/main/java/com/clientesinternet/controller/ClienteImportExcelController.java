package com.clientesinternet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.clientesinternet.service.ClienteImportExcelService;

@RestController
@RequestMapping("clientes")
public class ClienteImportExcelController {

    @Autowired
    private ClienteImportExcelService importService;

    @PostMapping("importar")
    public ResponseEntity<String> importarExcel(@RequestParam("file") MultipartFile file) {
        try {
            int cantidad = importService.importarClientes(file);
            return ResponseEntity.ok(String.valueOf(cantidad));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al importar Excel: " + e.getMessage());
        }
    }
}


