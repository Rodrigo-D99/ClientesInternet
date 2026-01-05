package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import service.ClienteImportExcelService;

@RestController
@RequestMapping("clientes")
public class ClienteImportExcelController {
    @Autowired
    private final ClienteImportExcelService service;

    public ClienteImportExcelController(ClienteImportExcelService service) {
        this.service = service;
    }

    @PostMapping("import")
    public String importarClientes(@RequestParam("file") MultipartFile file) {
        int total = service.importarClientes(file);
        return "Clientes importados: " + total;
    }
}

