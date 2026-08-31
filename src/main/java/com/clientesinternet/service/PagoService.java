package com.clientesinternet.service;

import com.clientesinternet.dto.req.PagoReq;
import com.clientesinternet.dto.resp.PagoResp;
import com.clientesinternet.dto.resp.ResumenMensualResp;
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

    // Manejo de la saldación de la Instalación
    if (Boolean.TRUE.equals(req.getSaldaInstalacion())) {
        Number costoRaw = cliente.getCostoInstalacion();
        BigDecimal costoInstalacion = (costoRaw != null) 
                ? BigDecimal.valueOf(costoRaw.doubleValue()) 
                : BigDecimal.ZERO;
        
        montoParaMensualidad = montoTotalPagado.subtract(costoInstalacion).max(BigDecimal.ZERO);
        
        cliente.setDeudaInstalacion("NO");
        cliente.setCostoInstalacion(0);
    }

    BigDecimal precioMensual = calcularPrecioMensual(cliente, medioPago);
    int mesesDeudaAnterior = cliente.calcularMesesAdeudados();
    BigDecimal saldoPendientePrevio = BigDecimal.valueOf(cliente.getSaldoPendiente() != null ? cliente.getSaldoPendiente() : 0.0);

    // 1. Deuda TOTAL previa acumulada en pesos (ej: 3 meses * $30.000 = $90.000)
    BigDecimal deudaAnteriorTotal = precioMensual.multiply(BigDecimal.valueOf(mesesDeudaAnterior)).add(saldoPendientePrevio);

    // 2. Restar el pago ingresado a la DEUDA TOTAL acumulada (ej: $90.000 - $20.000 = $70.000)
    BigDecimal nuevaDeudaTotal = deudaAnteriorTotal.subtract(montoParaMensualidad).max(BigDecimal.ZERO);

    // 3. Descomponer la deuda restante en Meses Completos + Saldo Pendiente
    int mesesDeudaRestantes = 0;
    BigDecimal nuevoSaldoPendiente = BigDecimal.ZERO;

    if (precioMensual.compareTo(BigDecimal.ZERO) > 0) {
        // Ej: $70.000 / $30.000 = 2 meses restantes, sobrante (remainder) = $10.000
        mesesDeudaRestantes = nuevaDeudaTotal.divideToIntegralValue(precioMensual).intValue();
        nuevoSaldoPendiente = nuevaDeudaTotal.remainder(precioMensual);
    }

    // 4. Actualizar la entidad Cliente
    cliente.setSaldoPendiente(nuevoSaldoPendiente.doubleValue());
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
            .saldoPendiente(nuevoSaldoPendiente)
            .mesesDeudaRestantes(mesesDeudaRestantes)
            .advertencia(advertencia.orElse(null))
            .build();
}

@Transactional
public void editarPago(Long id, PagoReq req) {
    Pago pago = pagoRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + id));

    Cliente cliente = pago.getCliente();
    BigDecimal precioMensual = calcularPrecioMensual(cliente, pago.getMedioPago());

    // 1. Calcular la diferencia entre el monto original y el nuevo monto
    BigDecimal montoViejo = (pago.getMonto() != null) ? pago.getMonto() : BigDecimal.ZERO;
    BigDecimal montoNuevo = (req.getMonto() != null) ? req.getMonto() : BigDecimal.ZERO;
    BigDecimal diferenciaMonto = montoNuevo.subtract(montoViejo); // Ej: $30.000 - $20.000 = +$10.000

    // 2. Obtener la deuda actual registrada en el cliente
    int mesesDeudaActuales = cliente.calcularMesesAdeudados();
    BigDecimal saldoPendienteActual = BigDecimal.valueOf(cliente.getSaldoPendiente() != null ? cliente.getSaldoPendiente() : 0.0);
    BigDecimal deudaActualTotal = precioMensual.multiply(BigDecimal.valueOf(mesesDeudaActuales)).add(saldoPendienteActual);

    // 3. Ajustar la deuda del cliente con la diferencia del pago editado
    BigDecimal nuevaDeudaTotal = deudaActualTotal.subtract(diferenciaMonto).max(BigDecimal.ZERO);

    // 4. Recalcular meses restantes y nuevo saldo pendiente
    int mesesDeudaRestantes = 0;
    BigDecimal nuevoSaldoPendiente = BigDecimal.ZERO;

    if (precioMensual.compareTo(BigDecimal.ZERO) > 0) {
        mesesDeudaRestantes = nuevaDeudaTotal.divideToIntegralValue(precioMensual).intValue();
        nuevoSaldoPendiente = nuevaDeudaTotal.remainder(precioMensual);
    }

    // 5. Impactar los cambios en el cliente
    cliente.setSaldoPendiente(nuevoSaldoPendiente.doubleValue());
    cliente.setMesesAdeudadosInicial(mesesDeudaRestantes);
    clienteRepo.save(cliente);

    // 6. Actualizar el registro del Pago
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

    pago.setMonto(montoNuevo);
    if (req.getMedioPago() != null) {
        pago.setMedioPago(req.getMedioPago());
    }
    if (req.getNota() != null) {
        pago.setNota(req.getNota());
    }

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

    public ResumenMensualResp obtenerResumenMensual(int anio, int mes) {
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Pago> pagos = pagoRepo.findByFechaPagoBetween(inicio, fin);
        ResumenMensualResp resumen = new ResumenMensualResp();

        if (pagos == null || pagos.isEmpty()) {
            return resumen;
        }

        Map<LocalDate, List<Pago>> pagosPorDia = pagos.stream()
                .filter(p -> p != null && p.getFechaPago() != null)
                .collect(Collectors.groupingBy(Pago::getFechaPago));

        pagosPorDia.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDate fecha = entry.getKey();
                    List<Pago> listaPagos = entry.getValue();

                    BigDecimal efectivoDia = listaPagos.stream()
                            .filter(p -> p.getMedioPago() == MedioPago.EFECTIVO)
                            .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal digitalDia = listaPagos.stream()
                            .filter(p -> p.getMedioPago() == MedioPago.TRANSFERENCIA || p.getMedioPago() == MedioPago.TARJETA)
                            .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    ResumenMensualResp.ResumenDiaDTO diaDTO = new ResumenMensualResp.ResumenDiaDTO();
                    diaDTO.setFecha(fecha.toString());
                    diaDTO.setEfectivo(efectivoDia);
                    diaDTO.setDigital(digitalDia);
                    diaDTO.setTotalDia(efectivoDia.add(digitalDia));

                    resumen.getDias().add(diaDTO);
                });

        BigDecimal totalEfectivo = resumen.getDias().stream()
                .map(ResumenMensualResp.ResumenDiaDTO::getEfectivo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDigital = resumen.getDias().stream()
                .map(ResumenMensualResp.ResumenDiaDTO::getDigital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        resumen.setTotalMesEfectivo(totalEfectivo);
        resumen.setTotalMesDigital(totalDigital);
        resumen.setTotalMesGeneral(totalEfectivo.add(totalDigital));

        return resumen;
    }
}