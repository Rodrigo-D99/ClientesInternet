package com.clientesinternet.service;

import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.MedioPago;
import com.clientesinternet.entity.Pago;
import com.clientesinternet.entity.PlanInternet;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.PagoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ClienteImportExcelService {
    
    private final ClienteRepository clienteRepo;
    private final PlanInternetRepository planRepo;
    private final PagoRepository pagoRepo; // Agregamos el repo de pagos
    private final DataFormatter dataFormatter = new DataFormatter();

    @Autowired
    public ClienteImportExcelService(ClienteRepository clienteRepo, PlanInternetRepository planRepo, PagoRepository pagoRepo) {
        this.clienteRepo = clienteRepo;
        this.planRepo = planRepo;
        this.pagoRepo = pagoRepo; // Lo inicializamos
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

                // Extraemos la cantidad de MB
                String mbString = getCell(row, 4);
                PlanInternet planAsignado = null;
                
                if (mbString != null) {
                    try {
                        int cantidadMB = (int) Double.parseDouble(mbString);
                        Optional<PlanInternet> planOpt = planRepo.findByCantidadMB(cantidadMB);
                        if (planOpt.isPresent()) {
                            planAsignado = planOpt.get();
                        }
                    } catch (NumberFormatException e) {
                        // Si no es un número, queda en null
                    }
                }
                String fibraTVStr = getCell(row, 5);
                boolean tieneFibraTV = fibraTVStr != null && fibraTVStr.equalsIgnoreCase("SI");
                String usuarioFibraTV = getCell(row, 6);
                String mesesPagadosStr = getCell(row, 7);
                int mesesPagados = (mesesPagadosStr != null) ? (int) Double.parseDouble(mesesPagadosStr) : 0;

                Cliente cliente = Cliente.builder()
                        .nombre(nombre)
                        .telefono(getCell(row, 1))
                        .direccion(getCell(row, 2))
                        .email(getCell(row, 3))
                        .plan(planAsignado)
                        .tieneFibraTV(tieneFibraTV)
                        .usuarioFibraTV(usuarioFibraTV)
                        .dni(getCell(row, 9))
                        .mesesPagados(mesesPagados)
                        .build();

                // 1. Guardamos el cliente primero para que tenga un ID válido
                clienteRepo.save(cliente);
                importados++;

                // 2. Procesamos el Historial de Pagos (Índice 14 = Columna O)
                String historialStr = getCell(row, 14); 
                
                if (historialStr != null && !historialStr.isBlank()) {
                    // Separamos cada pago usando el símbolo | (como es un caracter especial en Regex, usamos \\|)
                    String[] pagosArray = historialStr.split("\\|");
                    
                    for (String pStr : pagosArray) {
                        // Separamos los datos del pago usando el ; (el -1 es para que mantenga los campos vacíos si no hay nota)
                        String[] datosPago = pStr.split(";", -1); 
                        
                        if (datosPago.length >= 4) {
                            try {
                                Pago pago = Pago.builder()
                                        .cliente(cliente) // Vinculamos al nuevo cliente
                                        .fechaPago(LocalDate.parse(datosPago[0]))
                                        .monto(new BigDecimal(datosPago[1]))
                                        .medioPago(MedioPago.valueOf(datosPago[2]))
                                        .cantidadMeses(Integer.parseInt(datosPago[3]))
                                        .nota(datosPago.length > 4 ? datosPago[4] : null)
                                        .build();
                                
                                pagoRepo.save(pago);
                            } catch (Exception e) {
                                System.out.println("No se pudo importar un pago específico para el cliente " + nombre);
                            }
                        }
                    }
                }
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