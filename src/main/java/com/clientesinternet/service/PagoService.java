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

        Pago pago = Pago.builder()
                .cliente(cliente)
                .monto(req.getMonto())
                .medioPago(req.getMedioPago())
                .dniPagador(req.getDniPagador())
                .nota(req.getNota())
                .fechaPago(LocalDate.now())
                .periodoPagado(YearMonth.now())
                .build();

        pagoRepo.save(pago);
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
