package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.ClienteExportExcelService;
@RestController
@RequestMapping("/clientes")
public class ClienteExportExcelController {
    @Autowired
    private ClienteExportExcelService service;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) Boolean deudores,
            @RequestParam(required = false) String nombre
    ) {
        byte[] excel = service.exportarExcel(deudores, nombre);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=clientes.xlsx")
                .body(excel);
    }

}
