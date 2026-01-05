package service;

import dto.req.ClienteReq;
import dto.resp.ClienteResp;
import entity.Cliente;
import entity.Pago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import repository.ClienteRepository;
import repository.PagoRepository;
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

        Pago ultimoPago = pagoRepo
                .findTopByClienteIdOrderByPeriodoPagadoDesc(cliente.getId())
                .orElse(null);

        int mesesAdeudados = (ultimoPago != null)
                ? calcularMesesAdeudados(ultimoPago.getPeriodoPagado())
                : calcularMesesAdeudados(null);

        boolean deuda = mesesAdeudados > 0;

        return new ClienteResp(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                deuda,
                mesesAdeudados,
                ultimoPago != null ? ultimoPago.getNota() : null
        );
    }


}
