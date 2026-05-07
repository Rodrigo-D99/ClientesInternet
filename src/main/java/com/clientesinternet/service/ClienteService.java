package com.clientesinternet.service;

import com.clientesinternet.dto.req.ClienteReq;
import com.clientesinternet.dto.resp.ClienteResp;
import com.clientesinternet.entity.Cliente;
import com.clientesinternet.entity.MedioPago;
import com.clientesinternet.entity.Pago;
import com.clientesinternet.entity.PlanInternet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.repository.PagoRepository;
import com.clientesinternet.repository.PlanInternetRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service("ClienteService")
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private PagoRepository pagoRepo;
    @Autowired
    private PlanInternetRepository planRepo;

    public ClienteService(ClienteRepository clienteRepo, PagoRepository pagoRepo, PlanInternetRepository planRepo) {
        this.clienteRepo = clienteRepo;
        this.pagoRepo = pagoRepo;
        this.planRepo = planRepo;
    }

    @Transactional
    public ClienteResp save(ClienteReq req) {
        // Buscamos el plan por ID (el valor que viene en req.getCantidadMB())
        PlanInternet plan = null;
        if (req.getCantidadMB() != null) {
            plan = planRepo.findById(req.getCantidadMB().longValue()).orElse(null);
        }

        Cliente cliente = Cliente.builder()
                .nombre(req.getNombre())
                .telefono(req.getTelefono())
                .direccion(req.getDireccion())
                .tieneTV(req.getTieneTV() != null ? req.getTieneTV() : false)
                .tieneFibraTV(req.getTieneFibraTV() != null ? req.getTieneFibraTV() : false)
                .usuarioFibraTV(req.getUsuarioFibraTV())
                .dni(req.getDni())
                .plan(plan) // Asignamos el objeto Plan completo
                .esDemo(req.getEsDemo() != null ? req.getEsDemo() : false)
                .fechaVencimientoDemo(req.getFechaVencimientoDemo())
                .build();

        return castToResponse(clienteRepo.save(cliente));
    }

    public ClienteResp update(Long id, ClienteReq req) {
        Cliente cliente = clienteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        cliente.setNombre(req.getNombre());
        cliente.setTelefono(req.getTelefono());
        cliente.setDireccion(req.getDireccion());
        cliente.setTieneTV(req.getTieneTV());
        cliente.setTieneFibraTV(req.getTieneFibraTV());
        cliente.setUsuarioFibraTV(req.getUsuarioFibraTV());
        cliente.setDni(req.getDni());
        cliente.setEsDemo(req.getEsDemo());
        cliente.setFechaVencimientoDemo(req.getFechaVencimientoDemo());

        if (req.getCantidadMB() != null) {
            PlanInternet plan = planRepo.findById(req.getCantidadMB().longValue()).orElse(null);
            cliente.setPlan(plan);
        }

        return castToResponse(clienteRepo.save(cliente));
    }

    public Page<ClienteResp> findPaged(
            int page,
            int size,
            Boolean deudores,
            String nombre,
            String sort,
            String dir
    ) {
        System.out.println("soloDeudores recibido: " + deudores);
        System.out.println("Tipo: " + (deudores != null ? deudores.getClass().getName() : "null"));

        Set<String> SORT_DB = Set.of("nombre", "direccion");

        Sort sortObj = Sort.unsorted();

        if (SORT_DB.contains(sort)) {
            sortObj = Sort.by(
                    "desc".equalsIgnoreCase(dir)
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC,
                    sort
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObj
        );

        Page<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepo.findAll(pageable)
                : clienteRepo.findByNombreContainingIgnoreCase(nombre, pageable);

        List<ClienteResp> filtrados = clientes.stream()
                .map(this::castToResponse)
                .filter(c -> {
                    if (deudores == null) {
                        return true; // Todos
                    }
                    if (deudores) {
                        return c.getMesesAdeudados() > 0; // Solo deudores
                    } else {
                        return c.getMesesAdeudados() <= 0; // Solo al día
                    }
                })
                .toList();
        if ("mesesAdeudados".equals(sort)) {
            Comparator<ClienteResp> comp = Comparator.comparingInt(ClienteResp::getMesesAdeudados);
            if ("desc".equalsIgnoreCase(dir)) comp = comp.reversed();
            filtrados = filtrados.stream().sorted(comp).toList();
        }
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
    public List<ClienteResp> buscarClientesExcel(Boolean soloDeudores, String nombre) {

        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepo.findAll(Sort.by("nombre").ascending())
                : clienteRepo.findByNombreContainingIgnoreCase(
                nombre, Sort.by("nombre").ascending());

        return clientes.stream()
                .map(this::castToResponse)
                .filter(c ->
                        soloDeudores == null ||
                                !soloDeudores ||
                                c.getMesesAdeudados() > 0
                )
                .toList();
    }

    private ClienteResp castToResponse(Cliente cliente) {

        // 1. Obtener el último pago
        Pago ultimoPago = pagoRepo
                .findTopByClienteIdOrderByPeriodoPagadoDesc(cliente.getId())
                .orElse(null);

        // 2. Calcular deuda (si no hay pago, se calcula desde 'null')
        int mesesAdeudados = (ultimoPago != null)
                ? calcularMesesAdeudados(ultimoPago.getPeriodoPagado())
                : calcularMesesAdeudados(null);

        boolean deuda = mesesAdeudados > 0;

        // 3. Lógica inteligente de DNI
        // Prioridad: 1. El DNI de la ficha del cliente | 2. El DNI del último pago (si fue Transf/Tarjeta)
        String dni = (cliente.getDni() != null && !cliente.getDni().isBlank()) 
                ? cliente.getDni() 
                : ((ultimoPago != null && (ultimoPago.getMedioPago() == MedioPago.TRANSFERENCIA 
                    || ultimoPago.getMedioPago() == MedioPago.TARJETA)) 
                    ? ultimoPago.getDniPagador() : null);

        // 4. Mapeo de valores básicos
        String medioPago = (ultimoPago != null) ? ultimoPago.getMedioPago().name() : null;
        int mesesPagados = (cliente.getMesesPagados() != null) ? cliente.getMesesPagados() : 0;
        boolean tieneFibraTV = (cliente.getTieneFibraTV() != null) ? cliente.getTieneFibraTV() : false;
        boolean tieneTV = (cliente.getTieneTV() != null) ? cliente.getTieneTV() : false;
        boolean esDemo = (cliente.getEsDemo() != null) ? cliente.getEsDemo() : false;

        // 5. Construcción del objeto (Manten el orden del constructor de ClienteResp)
        return new ClienteResp(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                deuda,
                mesesAdeudados,
                mesesPagados,
                tieneFibraTV,
                cliente.getUsuarioFibraTV(),
                tieneTV,
                esDemo,
                cliente.getFechaVencimientoDemo(),
                medioPago,
                dni,
                (ultimoPago != null) ? ultimoPago.getNota() : null,
                (ultimoPago != null) ? ultimoPago.getFechaPago() : null,
                (ultimoPago != null) ? ultimoPago.getMonto() : null,
                (cliente.getPlan() != null) ? cliente.getPlan().getCantidadMB() : null
        );
    }

    public void delete(Long id) {
        clienteRepo.delete(clienteRepo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado")));
    }
}
