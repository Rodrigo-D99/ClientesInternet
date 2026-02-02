package com.clientesinternet.service;

import com.clientesinternet.dto.req.ClienteReq;
import com.clientesinternet.dto.resp.ClienteResp;
import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.MedioPago;
import com.clientesinternet.entity.Pago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.PagoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service("ClienteService")
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private PagoRepository pagoRepo;

    public ClienteService(ClienteRepository clienteRepo, PagoRepository pagoRepo) {
        this.clienteRepo = clienteRepo;
        this.pagoRepo = pagoRepo;
    }

    @Transactional
    public ClienteResp save(ClienteReq req) {
        Cliente cliente = Cliente.builder()
                .nombre(req.getNombre())
                .telefono(req.getTelefono())
                .direccion(req.getDireccion())
                .build();

        cliente = clienteRepo.save(cliente);
        return castToResponse(cliente);
    }
    @Transactional
    public ClienteResp update(Long id, ClienteReq req) {
        Cliente c = clienteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (req.getNombre() != null) {
            c.setNombre(req.getNombre());
        }
        if (req.getTelefono() != null) {
            c.setTelefono(req.getTelefono());
        }
        if (req.getDireccion() != null) {
            c.setDireccion(req.getDireccion());
        }
        clienteRepo.save(c);
        return castToResponse(c);
    }

    private boolean pagoMesActual(Long clienteId) {
        LocalDate now = LocalDate.now();
        LocalDate inicio = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate fin = now.with(TemporalAdjusters.lastDayOfMonth());

        return pagoRepo.findFirstByClienteIdAndFechaPagoBetween(clienteId, inicio, fin).isPresent();
    }



    public Page<ClienteResp> findPaged(
            int page,
            int size,
            Boolean soloDeudores,
            String nombre
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("nombre").ascending()
        );

        Page<Cliente> clientes = clienteRepo.findAll(pageable);

        List<ClienteResp> filtrados = clientes.stream()
                .map(this::castToResponse)
                .filter(c -> soloDeudores == null || !soloDeudores || c.isTieneDeuda())
                .filter(c -> nombre == null ||
                        c.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();

        return new PageImpl<>(
                filtrados,
                pageable,
                clientes.getTotalElements()
        );
    }
    public ClienteResp findById (Long id){
       Cliente cliente = clienteRepo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return castToResponse(cliente);
    }
    public List<ClienteResp> buscarClientesDeudores(Boolean soloDeudores, String nombre) {

        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepo.findAll()
                : clienteRepo.findByNombreContainingIgnoreCase(nombre);

        return clientes.stream()
                .map(this::castToResponse)
                .filter(r ->
                        !Boolean.TRUE.equals(soloDeudores) || r.getMesesAdeudados() > 0
                )
                .toList();
    }


    private int calcularMesesAdeudados(YearMonth ultimoPeriodoPagado) {
        YearMonth actual = YearMonth.now();

        if (ultimoPeriodoPagado == null) {
            return 1; // nunca pagó
        }

        return (int) ChronoUnit.MONTHS.between(
                ultimoPeriodoPagado.plusMonths(1),
                actual.plusMonths(1)
        );
    }

    private ClienteResp castToResponse(Cliente cliente) {

        // Obtener el último pago del cliente
        Pago ultimoPago = pagoRepo
                .findTopByClienteIdOrderByPeriodoPagadoDesc(cliente.getId())
                .orElse(null);

        // Calcular meses adeudados
        int mesesAdeudados = (ultimoPago != null)
                ? calcularMesesAdeudados(ultimoPago.getPeriodoPagado())
                : calcularMesesAdeudados(null);

        boolean deuda = mesesAdeudados > 0;

        // Preparar medio de pago y DNI si existe pago
        String medioPago = ultimoPago != null ? ultimoPago.getMedioPago().name() : null;
        String dni = (ultimoPago != null && (ultimoPago.getMedioPago() == MedioPago.TRANSFERENCIA
                || ultimoPago.getMedioPago() == MedioPago.TARJETA))
                ? ultimoPago.getDniPagador()
                : null;


        return new ClienteResp(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                deuda,
                mesesAdeudados,
                medioPago,
                dni,
                ultimoPago != null ? ultimoPago.getNota() : null,
                ultimoPago != null ? ultimoPago.getFechaPago() : null,
                ultimoPago != null ? ultimoPago.getMonto() : null
        );
    }


    public void delete(Long id) {
        clienteRepo.delete(clienteRepo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado")));
    }
}
