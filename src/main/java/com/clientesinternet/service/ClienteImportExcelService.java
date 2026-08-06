package com.clientesinternet.service;

import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.PlanInternet;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.PlanInternetRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ClienteImportExcelService {
    
    private final ClienteRepository clienteRepo;
    private final PlanInternetRepository planRepo;
    private final DataFormatter dataFormatter = new DataFormatter();

    @Autowired
    public ClienteImportExcelService(ClienteRepository clienteRepo, PlanInternetRepository planRepo) {
        this.clienteRepo = clienteRepo;
        this.planRepo = planRepo;
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

                // Extraemos la cantidad de MB (Columna D - Índice 3)
                String mbString = getCell(row, 3);
                PlanInternet planAsignado = null;
                
                if (mbString != null) {
                    try {
                        int cantidadMB = (int) Double.parseDouble(mbString);
                        // Buscamos el plan en la base de datos según los MB del Excel
                        Optional<PlanInternet> planOpt = planRepo.findByCantidadMB(cantidadMB);
                        if (planOpt.isPresent()) {
                            planAsignado = planOpt.get();
                        }
                    } catch (NumberFormatException e) {
                        // Si no es un número, queda en null
                    }
                }
                String fibraTVStr = getCell(row, 4);
                boolean tieneFibraTV = fibraTVStr != null && fibraTVStr.equalsIgnoreCase("SI");
                String usuarioFibraTV = getCell(row, 5);

                Cliente cliente = Cliente.builder()
                        .nombre(nombre)
                        .telefono(getCell(row, 1))
                        .direccion(getCell(row, 2))
                        .plan(planAsignado)
                        .tieneFibraTV(tieneFibraTV)
                        .usuarioFibraTV(usuarioFibraTV)
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
        String value = dataFormatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }
}