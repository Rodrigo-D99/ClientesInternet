package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ClienteExportExcelService;

@RestController
@RequestMapping("clientes")
public class ClienteExportExcelController {

    @Autowired
    private ClienteExportExcelService service;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) Boolean deudores,
            @RequestParam(required = false) String nombre
    ) {
        byte[] excel = service.generarExcel(deudores, nombre);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clientes.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excel.length)
                .body(excel);
    }
}
