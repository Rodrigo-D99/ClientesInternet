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

        List<ClienteResp> clientes = clienteService.buscarClientesDeudores(soloDeudores, nombre);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Clientes");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
            header.createCell(1).setCellValue("Dirección");
            header.createCell(2).setCellValue("Meses adeudados");
            header.createCell(3).setCellValue("Nota");

            int row = 1;
            for (ClienteResp c : clientes) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(c.getNombre());
                r.createCell(1).setCellValue(c.getDireccion());
                r.createCell(2).setCellValue(c.getMesesAdeudados());
                r.createCell(3).setCellValue(c.getNota());
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel", e);
        }
    }
}
