package com.clientesinternet.service;

import com.clientesinternet.dto.req.PagoReq;
import com.clientesinternet.dto.resp.PagoResp;
import com.clientesinternet.dto.resp.PagoHistorialResp;
import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.entity.MedioPago;
import com.clientesinternet.entity.Pago;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.ConfiguracionRepository;
import com.clientesinternet.repository.PagoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PagoService {

    private final PagoRepository pagoRepo;
    private final ClienteRepository clienteRepo;
    private final ConfiguracionRepository configuracionRepository;

    @Autowired
    public PagoService(PagoRepository pagoRepo, ClienteRepository clienteRepo, ConfiguracionRepository configuracionRepository) {
        this.pagoRepo = pagoRepo;
        this.clienteRepo = clienteRepo;
        this.configuracionRepository = configuracionRepository;
    }

    public BigDecimal calcularPrecioMensual(Cliente cliente, MedioPago medioPago) {
        BigDecimal precioBase = BigDecimal.ZERO;

        if (cliente.getPlan() != null) {
            Double precioPlan = (medioPago == MedioPago.EFECTIVO) 
                    ? cliente.getPlan().getPrecioEfectivo() 
                    : cliente.getPlan().getPrecioTransferencia();

            if (precioPlan != null) {
                precioBase = precioBase.add(BigDecimal.valueOf(precioPlan));
            }
        }

        if (Boolean.TRUE.equals(cliente.getTieneFibraTV())) {
            precioBase = precioBase.add(obtenerPrecioConfigurado("PRECIO_FIBRA_TV")); 
        }

        if (Boolean.TRUE.equals(cliente.getTieneTV())) {
            precioBase = precioBase.add(obtenerPrecioConfigurado("PRECIO_CABLE_TV"));
        }

        return precioBase;
    }
    private BigDecimal obtenerPrecioConfigurado(String clave) {
    return configuracionRepository.findById(clave)
            .map(Configuracion::getValor)
            .map(BigDecimal::valueOf)
            .orElse(BigDecimal.ZERO);
    }
    @Transactional
    public PagoResp registrarPago(Long clienteId, PagoReq req) {

        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        MedioPago medioPago = (req.getMedioPago() != null) ? req.getMedioPago() : MedioPago.EFECTIVO;
        int cantidadMeses = (req.getCantidadMeses() != null && req.getCantidadMeses() > 0) ? req.getCantidadMeses() : 1;
        
        BigDecimal montoTotalPagado = (req.getMonto() != null) ? req.getMonto() : BigDecimal.ZERO;
        BigDecimal montoParaMensualidad = montoTotalPagado;

        // Manejo de la saldación de la Instalación con control de Null Pointer Exception
        if (Boolean.TRUE.equals(req.getSaldaInstalacion())) {
            Number costoRaw = cliente.getCostoInstalacion();
            BigDecimal costoInstalacion = (costoRaw != null) 
                    ? BigDecimal.valueOf(costoRaw.doubleValue()) 
                    : BigDecimal.ZERO;
            
            // Se descuenta el valor de la instalación del dinero entregado
            montoParaMensualidad = montoTotalPagado.subtract(costoInstalacion).max(BigDecimal.ZERO);
            
            // Se resetea el estado en la base de datos para este cliente
            cliente.setDeudaInstalacion("NO");
            cliente.setCostoInstalacion(0);
        }

        BigDecimal precioMensual = calcularPrecioMensual(cliente, medioPago);
        int mesesDeudaAnterior = cliente.calcularMesesAdeudados();
        BigDecimal deudaAnteriorTotal = precioMensual.multiply(BigDecimal.valueOf(mesesDeudaAnterior));

        BigDecimal saldoPendiente = deudaAnteriorTotal.subtract(montoParaMensualidad).max(BigDecimal.ZERO);

        int mesesDeudaRestantes = 0;
        if (precioMensual.compareTo(BigDecimal.ZERO) > 0) {
            mesesDeudaRestantes = saldoPendiente.divide(precioMensual, 0, RoundingMode.CEILING).intValue();
        }

        cliente.setMesesAdeudadosInicial(mesesDeudaRestantes);
        int pagadosAnteriores = (cliente.getMesesPagados() != null) ? cliente.getMesesPagados() : 0;
        cliente.setMesesPagados(pagadosAnteriores + cantidadMeses);
        clienteRepo.save(cliente);

        YearMonth periodoInicio = (mesesDeudaAnterior > 0) 
                ? YearMonth.now().minusMonths(mesesDeudaAnterior) 
                : YearMonth.now();
        
        int mesesAvanzar = Math.max(0, cantidadMeses - 1);
        YearMonth periodoPagado = periodoInicio.plusMonths(mesesAvanzar);

        Pago pago = Pago.builder()
                .cliente(cliente)
                .monto(montoTotalPagado) 
                .medioPago(medioPago)
                .cantidadMeses(cantidadMeses)
                .dniPagador(req.getDniPagador())
                .nota(req.getNota())
                .fechaPago(LocalDate.now())
                .periodoPagado(periodoPagado)
                .build();

        pagoRepo.save(pago);

        Optional<String> advertencia = validarDni(req);

        return PagoResp.builder()
                .pagoId(pago.getId())
                .montoPagado(montoTotalPagado)
                .precioMensualBase(precioMensual)
                .deudaAnteriorTotal(deudaAnteriorTotal)
                .saldoPendiente(saldoPendiente)
                .mesesDeudaRestantes(mesesDeudaRestantes)
                .advertencia(advertencia.orElse(null))
                .build();
    }

    @Transactional
    public void editarPago(Long id, PagoReq req) {
        Pago pago = pagoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + id));

        if (req.getCantidadMeses() != null && req.getCantidadMeses() > 0) {
            int mesesAnteriores = pago.getCantidadMeses() != null ? pago.getCantidadMeses() : 0;
            int mesesNuevos = req.getCantidadMeses();
            YearMonth periodoAnterior = pago.getPeriodoPagado();
            if (periodoAnterior != null) {
                YearMonth inicioPeriodo = periodoAnterior.minusMonths(Math.max(mesesAnteriores - 1, 0));
                pago.setPeriodoPagado(inicioPeriodo.plusMonths(mesesNuevos - 1L));
            }
            pago.setCantidadMeses(mesesNuevos);
        }

        pago.setMonto(req.getMonto());
        if (req.getMedioPago() != null) {
            pago.setMedioPago(req.getMedioPago());
        }
        pago.setNota(req.getNota());

        pagoRepo.save(pago);
    }

    @Transactional
    public void actualizarNota(Long clienteId, String nota) {
        Pago pago = pagoRepo.findByClienteId(clienteId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        pago.setNota(nota);
    }

    private Optional<String> validarDni(PagoReq req) {
        if (req.getMedioPago() != MedioPago.EFECTIVO &&
                (req.getDniPagador() == null || req.getDniPagador().isBlank())) {

            return Optional.of("Pago no efectivo sin DNI. Recordar solicitarlo.");
        }
        return Optional.empty();
    }

    public List<PagoHistorialResp> obtenerHistorial(Long clienteId) {
        return pagoRepo.findByClienteIdOrderByFechaPagoDescIdDesc(clienteId)
                .stream()
                .map(p -> new PagoHistorialResp(
                    p.getId(),
                    p.getFechaPago(),
                    p.getMonto(),
                    p.getMedioPago().name(),
                    p.getCantidadMeses(),
                    p.getNota()
                )).collect(Collectors.toList());
    }

    public Map<String, Object> obtenerEstadisticasHoy() {
        LocalDate hoy = LocalDate.now();
        List<Pago> pagosHoy = pagoRepo.findByFechaPago(hoy);

        long cantidadCobros = pagosHoy.size();
        BigDecimal totalRecaudado = pagosHoy.stream()
                                            .map(Pago::getMonto)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("cantidadCobros", cantidadCobros);
        stats.put("totalRecaudado", totalRecaudado);

        return stats;
    }
}