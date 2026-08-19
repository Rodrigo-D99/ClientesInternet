package com.clientesinternet.service;

import com.clientesinternet.dto.req.PagoReq;
import com.clientesinternet.dto.resp.PagoHistorialResp;
import com.clientesinternet.dto.resp.PagoResp;
import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.MedioPago;
import com.clientesinternet.entity.Pago;
import org.springframework.beans.factory.annotation.Autowired;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.PagoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PagoService {
    @Autowired
    private final PagoRepository pagoRepo;
    @Autowired
    private final ClienteRepository clienteRepo;

    public PagoService(PagoRepository pagoRepo, ClienteRepository clienteRepo) {
        this.pagoRepo = pagoRepo;
        this.clienteRepo = clienteRepo;
    }

    @Transactional
    public PagoResp registrarPago(Long clienteId, PagoReq req) {

        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Optional<String> advertencia = validarDni(req);
        
        // Usar la cantidad de meses del request o 1 por defecto
        Integer cantidadMeses = req.getCantidadMeses() != null && req.getCantidadMeses() > 0 ? 
                req.getCantidadMeses() : 1;

        // Obtener el último pago para calcular desde dónde suma
        Pago ultimoPago = pagoRepo
                .findTopByClienteIdOrderByPeriodoPagadoDesc(clienteId)
                .orElse(null);

        // Calcular el período hasta el que quedará pagado
        YearMonth periodoPagadoHasta;
        YearMonth ahora = YearMonth.now();
        
        if (ultimoPago != null && ultimoPago.getPeriodoPagado() != null && 
            ultimoPago.getPeriodoPagado().isAfter(ahora)) {
            // Si ya está pagado en meses futuros, suma desde donde quedó
            periodoPagadoHasta = ultimoPago.getPeriodoPagado().plusMonths(cantidadMeses);
        } else {
            // Si no está pagado o el pago es antiguo, suma desde ahora
            periodoPagadoHasta = ahora.plusMonths(cantidadMeses - 1);
        }

        Pago pago = Pago.builder()
                .cliente(cliente)
                .monto(req.getMonto())
                .medioPago(req.getMedioPago())
                .cantidadMeses(cantidadMeses)
                .dniPagador(req.getDniPagador())
                .nota(req.getNota())
                .fechaPago(LocalDate.now())
                .periodoPagado(periodoPagadoHasta)
                .build();

        pagoRepo.save(pago);
        
        // Actualizar el total de meses pagados del cliente (acumulativo)
        cliente.setMesesPagados((cliente.getMesesPagados() != null ? cliente.getMesesPagados() : 0) + cantidadMeses);
        clienteRepo.save(cliente);
        
        return new PagoResp(
                pago.getId(),
                advertencia.orElse(null)
        );
    }
    @Transactional
    public void editarPago(Long id, PagoReq req) {
        // 1. Buscamos el pago existente
        Pago pago = pagoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + id));

        // 2. Calculamos la diferencia de meses si se modificó la cantidad
        if (req.getCantidadMeses() != null && req.getCantidadMeses() > 0) {
            int mesesAnteriores = pago.getCantidadMeses() != null ? pago.getCantidadMeses() : 0;
            int mesesNuevos = req.getCantidadMeses();
            int diferenciaMeses = mesesNuevos - mesesAnteriores;

            // Si hubo cambios en la cantidad de meses, actualizamos el acumulado del cliente
            if (diferenciaMeses != 0) {
                Cliente cliente = pago.getCliente();
                int mesesPagadosActuales = cliente.getMesesPagados() != null ? cliente.getMesesPagados() : 0;
                
                // Actualizamos y guardamos el cliente
                cliente.setMesesPagados(mesesPagadosActuales + diferenciaMeses);
                clienteRepo.save(cliente);
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
        
        // Obtenemos todos los pagos registrados con la fecha de hoy
        List<Pago> pagosHoy = pagoRepo.findByFechaPago(hoy);

        // Calculamos la cantidad y el monto total
        long cantidadCobros = pagosHoy.size();
        BigDecimal totalRecaudado = pagosHoy.stream()
                                            .map(Pago::getMonto)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Empaquetamos los datos para enviarlos
        Map<String, Object> stats = new HashMap<>();
        stats.put("cantidadCobros", cantidadCobros);
        stats.put("totalRecaudado", totalRecaudado);

        return stats;
    }
}
