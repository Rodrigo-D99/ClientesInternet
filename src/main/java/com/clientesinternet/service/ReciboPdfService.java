package com.clientesinternet.service;

import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.entity.Pago;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReciboPdfService {

    private final ClienteRepository clienteRepository;
    private final ConfiguracionRepository configuracionRepository;

    public ReciboPdfService(ClienteRepository clienteRepository, ConfiguracionRepository configuracionRepository) {
        this.clienteRepository = clienteRepository;
        this.configuracionRepository = configuracionRepository;
    }

    public byte[] generarPdfMasivo() throws Exception {
        List<Cliente> clientes = clienteRepository.findAll().stream()
                .filter(c -> c.getMesesPagados() != null)
                .toList();
        return generarDocumentoPdf(clientes);
    }

    public byte[] generarPdfIndividual(Long id) throws Exception {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return generarDocumentoPdf(Collections.singletonList(cliente));
    }

    private byte[] generarDocumentoPdf(List<Cliente> clientes) throws Exception {
        Document document = new Document(PageSize.A4, 30, 30, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontPequena = FontFactory.getFont(FontFactory.HELVETICA, 7);

        int count = 0;
        for (Cliente cliente : clientes) {
            
            Pago ultimoPago = null;
            if (cliente.getPagos() != null && !cliente.getPagos().isEmpty()) {
                ultimoPago = cliente.getPagos().stream()
                        .max(Comparator.comparing(Pago::getId))
                        .orElse(null);
            }

            double precioEfectivo = 0.0;
            double precioTransferencia = 0.0;
            Integer mbPlan = 0;

            if (cliente.getPlan() != null) {
                precioEfectivo = cliente.getPlan().getPrecioEfectivo();
                precioTransferencia = cliente.getPlan().getPrecioTransferencia();
                mbPlan = cliente.getPlan().getCantidadMB();
            }

            double costofibraTV = 0;
            double costoTV = 0;
            String textoTvDetalle = "";
            
            if (Boolean.TRUE.equals(cliente.getTieneFibraTV())) {
                costofibraTV = obtenerPrecioTV();
                textoTvDetalle = " + Fibra TV: $" + (int)costofibraTV;
            }
            
            String tieneTvCable = Boolean.TRUE.equals(cliente.getTieneTV()) ? "Sí" : "No";
            if (tieneTvCable.equals("Sí") && obtenerPrecioCableTV() > 0) {
                costoTV = obtenerPrecioCableTV();
                textoTvDetalle += " + TV por Cable: $" + (int)costoTV;
            }

            // Lógica de Instalación: Se separa la cobrada en este pago de la deuda informativa
            double costoInstalacionCobrado = 0.0;
            String textoInstalacion = null;

            if (ultimoPago != null && ultimoPago.getNota() != null && ultimoPago.getNota().contains("Pago Instalación")) {
                Matcher m = Pattern.compile("Pago Instalación \\((.*?)\\): \\$(\\d+(\\.\\d+)?)").matcher(ultimoPago.getNota());
                if (m.find()) {
                    String tipoInst = m.group(1);
                    costoInstalacionCobrado = Double.parseDouble(m.group(2));
                    textoInstalacion = "Abonó Instalación: " + tipoInst + " - $ " + String.format("%.2f", costoInstalacionCobrado);
                }
            } else if (cliente.getDeudaInstalacion() != null && !cliente.getDeudaInstalacion().equals("NO")) {
                double montoDeuda = (cliente.getCostoInstalacion() != null) ? cliente.getCostoInstalacion().doubleValue() : 0.0;
                textoInstalacion = "Debe Instalación: " + cliente.getDeudaInstalacion() + " - $ " + String.format("%.2f", montoDeuda);
            }

            PdfPTable tableRecibo = new PdfPTable(1);
            tableRecibo.setWidthPercentage(100);
            
            PdfPCell celda = new PdfPCell();
            celda.setPadding(10);
            celda.setBorderWidth(0.5f);

            String fechaActual = (ultimoPago != null && ultimoPago.getFechaPago() != null)
                    ? ultimoPago.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            Paragraph header = new Paragraph("RECIBO DE PAGO - " + fechaActual, fontTitulo);
            header.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(header);

            String direccion = cliente.getDireccion() != null ? cliente.getDireccion() : "---";
            String concepto = "Internet " + mbPlan + "MB" + textoTvDetalle;
            
            String periodoPagado = (ultimoPago != null && ultimoPago.getPeriodoPagado() != null)
                    ? ultimoPago.getPeriodoPagado().toString()
                    : YearMonth.now().toString();

            Paragraph body = new Paragraph();
            body.setFont(fontNormal);
            body.setLeading(11f);
            body.add(new Chunk("Cliente: ", fontTitulo));
            body.add(cliente.getNombre().toUpperCase() + " | Dir: " + direccion + " | Pago mes: " + periodoPagado + "\n");
            body.add(new Chunk("Concepto: ", fontTitulo));
            body.add(concepto + "\n");
            
            body.add(new Chunk("PRECIO INTERNET:\n", fontTitulo));
            body.add("• EFECTIVO : $ " + String.format("%.2f", precioEfectivo) + "\n");
            body.add("• TRANSFERENCIA / TARJETA: $ " + String.format("%.2f", precioTransferencia) + "\n");
            body.add(new Chunk("Tiene TV por cable: ", fontTitulo));
            body.add(tieneTvCable + "\n");
            
            if (textoInstalacion != null) {
                body.add(new Chunk(textoInstalacion.startsWith("Debe") ? "Debe Instalación: " : "Abonó Instalación: ", fontTitulo));
                body.add(textoInstalacion.replace("Debe Instalación: ", "").replace("Abonó Instalación: ", "") + "\n");
            }
            
            celda.addElement(body);

            PdfPTable tableMonto = new PdfPTable(1);
            tableMonto.setWidthPercentage(35);
            tableMonto.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            // Los totales solo incorporan la instalación SI la saldó en este cobro
            double totalEfec = precioEfectivo + costoTV + costofibraTV + costoInstalacionCobrado;
            double totalTrf = precioTransferencia + costoTV + costofibraTV + costoInstalacionCobrado;

            PdfPCell celdaMontoEfec = new PdfPCell(new Phrase("TOTAL EFEC: $ " + (int)totalEfec, fontTitulo));
            celdaMontoEfec.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            celdaMontoEfec.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaMontoEfec.setPadding(4);
            tableMonto.addCell(celdaMontoEfec);

            PdfPCell celdaMontoTranf = new PdfPCell(new Phrase("TOTAL TRANF: $ " + (int)totalTrf, fontTitulo));
            celdaMontoTranf.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            celdaMontoTranf.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaMontoTranf.setPadding(4);
            tableMonto.addCell(celdaMontoTranf);
            
            celda.addElement(tableMonto);
            celda.addElement(new Paragraph("Son: " + NumeroALetras.convertir(totalTrf) + " pesos.", fontPequena));

            tableRecibo.addCell(celda);
            document.add(tableRecibo);

            count++;
            if (count % 5 == 0 && count < clientes.size()) {
                document.newPage();
            } else {
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
            }
        }

        document.close();
        return out.toByteArray();
    }

    private double obtenerPrecioTV() {
        return configuracionRepository.findById("PRECIO_FIBRA_TV")
                .map(Configuracion::getValor)
                .orElse(0.0);
    }
    
    private double obtenerPrecioCableTV() {
        return configuracionRepository.findById("PRECIO_CABLE_TV")
                .map(Configuracion::getValor)
                .orElse(0.0);
    }
}