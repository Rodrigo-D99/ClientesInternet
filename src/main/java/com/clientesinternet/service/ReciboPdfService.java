package com.clientesinternet.service;

import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.ConfiguracionRepository;
import com.clientesinternet.util.NumeroALetras;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReciboPdfService {

    private final ClienteRepository clienteRepository;
    private final ConfiguracionRepository configuracionRepository;

    public ReciboPdfService(ClienteRepository clienteRepository, 
                               ConfiguracionRepository configuracionRepository) {
        this.clienteRepository = clienteRepository;
        this.configuracionRepository = configuracionRepository;
    }

    public byte[] generarPdfMasivo() throws Exception {
        // Filtramos deudores (puedes ajustar esta lógica según tu necesidad)
        List<Cliente> clientes = clienteRepository.findAll().stream()
                .filter(c -> c.getMesesPagados() != null)
                .toList();

        // Bajamos los márgenes del documento para que entren los 5 recibos
        Document document = new Document(PageSize.A4, 30, 30, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontPequena = FontFactory.getFont(FontFactory.HELVETICA, 7);

        int count = 0;
        for (Cliente cliente : clientes) {
            // 1. Precios Base
           double precioEfectivo = 0.0;
            double precioTransferencia = 0.0;
            Integer mbPlan = 0;

            if (cliente.getPlan() != null) {
                precioEfectivo = cliente.getPlan().getPrecioEfectivo();
                precioTransferencia = cliente.getPlan().getPrecioTransferencia();
                mbPlan = cliente.getPlan().getCantidadMB(); // Ahora sacamos los MB del plan
            }
            // 2. Lógica de Fibra TV (Suma y Detalle) + TV Cable
            double costoTV = 0;
            String textoTvDetalle = "";
            if (Boolean.TRUE.equals(cliente.getTieneFibraTV())) {
                costoTV = obtenerPrecioTV();
                textoTvDetalle = " + Fibra TV: $" + (int)costoTV;
            }
            String tieneTvCable= "";
            if (Boolean.TRUE.equals(cliente.getTieneTV())) {
                tieneTvCable = "Sí";
            } else {
                tieneTvCable = "No";
            }
            // 3. Tabla contenedor del recibo
            PdfPTable tableRecibo = new PdfPTable(1);
            tableRecibo.setWidthPercentage(100);
            
            PdfPCell celda = new PdfPCell();
            celda.setPadding(10);
            celda.setBorderWidth(0.5f); // Borde fino para ahorrar espacio

            // Cabecera
            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Paragraph header = new Paragraph("RECIBO DE PAGO - " + fechaActual, fontTitulo);
            header.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(header);

            // Cuerpo del recibo
            String direccion = cliente.getDireccion() != null ? cliente.getDireccion() : "---";
            String concepto = "Internet " + mbPlan + "MB" + textoTvDetalle;

            Paragraph body = new Paragraph();
            body.setFont(fontNormal);
            body.setLeading(11f); // Espaciado entre líneas compacto
            body.add(new Chunk("Cliente: ", fontTitulo));
            body.add(cliente.getNombre().toUpperCase() + " | Dir: " + direccion + " | Pago mes: "+YearMonth.now()+ "\n");
            body.add(new Chunk("Concepto: ", fontTitulo));
            body.add(concepto + "\n");
            
            // Sección de opciones de pago
            body.add(new Chunk("PRECIO INTERNET:\n", fontTitulo));
            body.add("• EFECTIVO : $ " + String.format("%.2f", precioEfectivo) + "\n");
            body.add("• TRANSFERENCIA / TARJETA: $ " + String.format("%.2f", precioTransferencia) + "\n");
            body.add(new Chunk("Tiene TV cable: ", fontTitulo));
            body.add(tieneTvCable + "\n");
            celda.addElement(body);

            // Cuadro de Total (Efectivo)
            PdfPTable tableMonto = new PdfPTable(1);
            tableMonto.setWidthPercentage(35);
            tableMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            PdfPCell celdaMontoEfec = new PdfPCell(new Phrase("TOTAL EFEC: $ " + ((int)precioEfectivo + (int)costoTV), fontTitulo));
            celdaMontoEfec.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            celdaMontoEfec.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaMontoEfec.setPadding(4);
            tableMonto.addCell(celdaMontoEfec);
            PdfPCell celdaMontoTranf = new PdfPCell(new Phrase("TOTAL TRANF: $ " + ((int)precioTransferencia + (int)costoTV), fontTitulo));
            celdaMontoTranf.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            celdaMontoTranf.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaMontoTranf.setPadding(4);
            tableMonto.addCell(celdaMontoTranf);
            celda.addElement(tableMonto);
            celda.addElement(new Paragraph("Son: " + NumeroALetras.convertir(precioTransferencia+costoTV) + " pesos.", fontPequena));

            tableRecibo.addCell(celda);
            document.add(tableRecibo);

            // 4. Salto de página y espaciado
            count++;
            if (count % 5 == 0 && count < clientes.size()) {
                document.newPage();
            } else {
                // Pequeño espacio entre recibos en la misma hoja
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
            }
        }

        document.close();
        return out.toByteArray();
    }


    private double obtenerPrecioTV() {
        // Busca el valor configurado con la clave 'precio_fibratv'
        return configuracionRepository.findById("PRECIO_FIBRA_TV")
                .map(Configuracion::getValor)
                .orElse(0.0);
    }
}