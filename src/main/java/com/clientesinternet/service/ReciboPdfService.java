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

@Service
public class ReciboPdfService {

    private final ClienteRepository clienteRepository;
    private final ConfiguracionRepository configuracionRepository;

    public ReciboPdfService(ClienteRepository clienteRepository, ConfiguracionRepository configuracionRepository) {
        this.clienteRepository = clienteRepository;
        this.configuracionRepository = configuracionRepository;
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
            // 1. Validar qué se pagó realmente en este último registro
            boolean pagoFibra = ultimoPago != null && Boolean.TRUE.equals(ultimoPago.getSaldaFibra());
            boolean pagoCable = ultimoPago != null && Boolean.TRUE.equals(ultimoPago.getSaldaCable());
            boolean pagoInstalacion = ultimoPago != null && Boolean.TRUE.equals(ultimoPago.getSaldaInstalacion());

            double costofibraTV = 0;
            double costoTV = 0;
            String textoTvDetalle = "";
            String textoTvPendiente = "";
            
            // 2. Lógica de Fibra TV
            if (Boolean.TRUE.equals(cliente.getTieneFibraTV())) {
                Integer cantCuentas = (cliente.getCantCuentasFibraTV() != null) ? cliente.getCantCuentasFibraTV() : 1;
                double precioTotalFibra = obtenerPrecioTV() * cantCuentas;
                
                if (pagoFibra) {
                    costofibraTV = precioTotalFibra;
                    textoTvDetalle += " + Fibra TV: $" + (int)costofibraTV + (cantCuentas > 1 ? " (" + cantCuentas + " cuentas)" : "");
                } else {
                    textoTvPendiente += "\n• Debe Fibra TV ($" + (int)precioTotalFibra + (cantCuentas > 1 ? " (" + cantCuentas + " cuentas)": ""+ ") - Se suma al próximo pago.");
                }
            }
            
            // 3. Lógica de TV por Cable
            String tieneTvCable = Boolean.TRUE.equals(cliente.getTieneTV()) ? "Sí" : "No";
            if ("Sí".equals(tieneTvCable) && obtenerPrecioCableTV() > 0) {
                if (pagoCable) {
                    costoTV = obtenerPrecioCableTV();
                    textoTvDetalle += " + TV por Cable: $" + (int)costoTV;
                } else {
                    textoTvPendiente += "\n• Debe TV Cable ($" + (int)obtenerPrecioCableTV() + ") - Se suma al próximo pago.";
                }
            }

            // 4. Lógica de Instalación
            double costoInstalacionCobrado = 0.0;
            String textoInstalacion = null;

            if (cliente.getDeudaInstalacion() != null && !cliente.getDeudaInstalacion().equals("NO")) {
                double montoDeuda = (cliente.getCostoInstalacion() != null) ? cliente.getCostoInstalacion().doubleValue() : 0.0;
                
                if (pagoInstalacion) {
                    costoInstalacionCobrado = montoDeuda;
                    textoInstalacion = "Abonó Instalación: " + cliente.getDeudaInstalacion() + " - $ " + String.format("%.2f", costoInstalacionCobrado);
                } else {
                    textoTvPendiente = "\n• Debe Instalación: " + cliente.getDeudaInstalacion() + " - ($ " + String.format("%.2f", montoDeuda)+")";
                }
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
            if (!textoTvPendiente.isEmpty()) {
                body.add(new Chunk("SERVICIOS PENDIENTES:", fontTitulo));
                body.add(textoTvPendiente + "\n");
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