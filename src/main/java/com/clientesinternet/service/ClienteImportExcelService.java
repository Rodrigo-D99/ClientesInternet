package com.clientesinternet.service;

import com.clientesinternet.entity.Cliente;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.clientesinternet.repository.ClienteRepository;

@Service
public class ClienteImportExcelService {
    
    private final ClienteRepository clienteRepo;

    @Autowired
    public ClienteImportExcelService(ClienteRepository clienteRepo) {
        this.clienteRepo = clienteRepo;
    }

    @Transactional
    public int importarClientes(MultipartFile file) throws Exception {

        int importados = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nombre = getCell(row, 0);
                if (nombre == null || nombre.isBlank()) continue;

                Cliente cliente = Cliente.builder()
                        .nombre(nombre)
                        .telefono(getCell(row, 1))
                        .direccion(getCell(row, 2))
                        .tieneFibraTV(false)
                        .build();

                clienteRepo.save(cliente);
                importados++;
            }

        } catch (Exception e) {
            throw new RuntimeException("Error leyendo Excel", e);
        }

        return importados;
    }

    private String getCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        String value = cell.toString().trim();
        return value.isEmpty() ? null : value;
    }
}

