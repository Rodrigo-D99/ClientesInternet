package com.clientesinternet.service;

import com.clientesinternet.dto.resp.ClienteResp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ClienteExportExcelService {

    @Autowired
    private ClienteService clienteService;

    public byte[] generarExcel(Boolean soloDeudores, String nombre) {

        List<ClienteResp> clientes = clienteService.buscarClientesExcel(soloDeudores, nombre);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Clientes");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
            header.createCell(1).setCellValue("Teléfono");
            header.createCell(2).setCellValue("Dirección");
            header.createCell(3).setCellValue("Cantidad MB");
            header.createCell(4).setCellValue("Meses pagados");
            header.createCell(5).setCellValue("Fibra TV");
            header.createCell(6).setCellValue("Usuario Fibra TV");
            header.createCell(7).setCellValue("Medio pago");
            header.createCell(8).setCellValue("DNI");
            header.createCell(9).setCellValue("Nota");
            header.createCell(10).setCellValue("Fecha último pago");
            header.createCell(11).setCellValue("Monto último pago");
            header.createCell(12).setCellValue("Meses adeudados");
            int row = 1;
            for (ClienteResp c : clientes) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(c.getNombre());
                r.createCell(1).setCellValue(c.getTelefono() != null ? c.getTelefono() : "");
                r.createCell(2).setCellValue(c.getDireccion());
                r.createCell(3).setCellValue(c.getCantidadMB() != null ? c.getCantidadMB() : 0);
                r.createCell(4).setCellValue(c.getMesesPagados());
                r.createCell(5).setCellValue(c.isTieneFibraTV() ? "Sí" : "No");
                r.createCell(6).setCellValue(c.getUsuarioFibraTV() != null ? c.getUsuarioFibraTV() : "");
                r.createCell(7).setCellValue(c.getMedioPago() != null ? c.getMedioPago() : "");
                r.createCell(8).setCellValue(c.getDni() != null ? c.getDni() : "");
                r.createCell(9).setCellValue(c.getNota() != null ? c.getNota() : "");
                r.createCell(10).setCellValue(c.getFechaUltimoPago() != null ? c.getFechaUltimoPago().toString() : "");
                r.createCell(11).setCellValue(c.getMontoUltimoPago() != null ? c.getMontoUltimoPago().doubleValue() : 0.0);
                r.createCell(12).setCellValue(c.getMesesAdeudados());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel", e);
        }
    }
}
