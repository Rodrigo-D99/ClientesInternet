package com.clientesinternet.service;

import com.clientesinternet.dto.resp.ClienteResp;
import com.clientesinternet.entity.Pago;
import com.clientesinternet.repository.PagoRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteExportExcelService {

    @Autowired
    private ClienteService clienteService;

    // Inyectamos el repositorio de pagos para buscar el historial
    @Autowired
    private PagoRepository pagoRepo;

    public byte[] generarExcel(Boolean soloDeudores, String nombre) {

        List<ClienteResp> clientes = clienteService.buscarClientesExcel(soloDeudores, nombre);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Clientes");

            Row header = sheet.createRow(0);
            int i = 0;
            header.createCell(i++).setCellValue("Nombre");
            header.createCell(i++).setCellValue("Teléfono");
            header.createCell(i++).setCellValue("Dirección");
            header.createCell(i++).setCellValue("Email");
            header.createCell(i++).setCellValue("Cantidad MB");
            header.createCell(i++).setCellValue("Fibra TV");
            header.createCell(i++).setCellValue("Usuario Fibra TV");
            header.createCell(i++).setCellValue("Meses pagados");
            header.createCell(i++).setCellValue("Medio pago");
            header.createCell(i++).setCellValue("DNI");
            header.createCell(i++).setCellValue("Nota");
            header.createCell(i++).setCellValue("Fecha último pago");
            header.createCell(i++).setCellValue("Monto último pago");
            header.createCell(i++).setCellValue("Meses adeudados");
            // NUEVA COLUMNA
            header.createCell(i++).setCellValue("Historial Pagos");

            int row = 1;
            for (ClienteResp c : clientes) {
                i = 0;
                Row r = sheet.createRow(row++);
                r.createCell(i++).setCellValue(c.getNombre());
                r.createCell(i++).setCellValue(c.getTelefono() != null ? c.getTelefono() : "");
                r.createCell(i++).setCellValue(c.getDireccion());
                r.createCell(i++).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                r.createCell(i++).setCellValue(c.getCantidadMB() != null ? c.getCantidadMB() : 0);
                r.createCell(i++).setCellValue(c.isTieneFibraTV() ? "Sí" : "No");
                r.createCell(i++).setCellValue(c.getUsuarioFibraTV() != null ? c.getUsuarioFibraTV() : "");
                r.createCell(i++).setCellValue(c.getMesesPagados());
                r.createCell(i++).setCellValue(c.getMedioPago() != null ? c.getMedioPago() : "");
                r.createCell(i++).setCellValue(c.getDni() != null ? c.getDni() : "");
                r.createCell(i++).setCellValue(c.getNota() != null ? c.getNota() : "");
                r.createCell(i++).setCellValue(c.getFechaUltimoPago() != null ? c.getFechaUltimoPago().toString() : "");
                r.createCell(i++).setCellValue(c.getMontoUltimoPago() != null ? c.getMontoUltimoPago().doubleValue() : 0.0);
                r.createCell(i++).setCellValue(c.getMesesAdeudados());

                // LÓGICA DEL HISTORIAL: Buscamos y concatenamos
                List<Pago> pagos = pagoRepo.findByClienteIdOrderByFechaPagoDescIdDesc(c.getId());
                
                String historialStr = pagos.stream().map(p -> 
                    p.getFechaPago() + ";" + 
                    p.getMonto() + ";" + 
                    p.getMedioPago().name() + ";" + 
                    p.getCantidadMeses() + ";" + 
                    (p.getNota() != null ? p.getNota() : "")
                ).collect(Collectors.joining("|"));

                r.createCell(i++).setCellValue(historialStr);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel", e);
        }
    }
}