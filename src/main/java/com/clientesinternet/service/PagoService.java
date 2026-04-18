package com.clientesinternet.service;

import com.clientesinternet.dto.req.PagoReq;
import com.clientesinternet.dto.resp.PagoResp;
import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.MedioPago;
import com.clientesinternet.entity.Pago;
import org.springframework.beans.factory.annotation.Autowired;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.PagoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

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


}
