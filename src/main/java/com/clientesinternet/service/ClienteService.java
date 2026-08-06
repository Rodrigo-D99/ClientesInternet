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
                .deudaInstalacion(req.getDeudaInstalacion() != null ? req.getDeudaInstalacion() : "NO")
                .costoInstalacion("NO".equalsIgnoreCase(req.getDeudaInstalacion()) ? 0 :
                     (req.getCostoInstalacion() != null ? req.getCostoInstalacion() : 0))
                .plan(plan)
                .esDemo(req.getEsDemo() != null ? req.getEsDemo() : false)
                .fechaVencimientoDemo(req.getFechaVencimientoDemo())
                .build();

        return castToResponse(clienteRepo.save(cliente));
    }

    @Transactional
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
        cliente.setDeudaInstalacion(req.getDeudaInstalacion());
        if ("NO".equalsIgnoreCase(req.getDeudaInstalacion())) {
            cliente.setCostoInstalacion(0);
        } else {
            cliente.setCostoInstalacion(req.getCostoInstalacion());
        }
        if (req.getCantidadMB() != null) {
            PlanInternet plan = planRepo.findById(req.getCantidadMB().longValue()).orElse(null);
            cliente.setPlan(plan);
        }

        return castToResponse(clienteRepo.save(cliente));
    }
    @Transactional
    public Page<ClienteResp> findPaged(
            int page,
            int size,
            Boolean deudores,
            String nombre,
            String sort,
            String dir
    ) {
        // 1. Definimos qué columnas SÍ existen en la Base de Datos para que SQL las ordene
        Set<String> columnasDB = Set.of(
            "nombre", "direccion", "mesesAdeudados", "mesesPagados", 
            "deudaInstalacion", "tieneFibraTV", "tieneTV", "costoInstalacion"
        );

        Sort sortObj = Sort.unsorted();
        boolean ordenarEnMemoria = false;

        // 2. ¿El campo pertenece a la DB o a Memoria?
        if (sort != null && columnasDB.contains(sort)) {
            // Lógica para SQL
            String sortField = "mesesAdeudados".equals(sort) ? "mesesPagados" : sort;
            if ("deudaInstalacion".equals(sort)) {
            sortField = "costoInstalacion";
            }
            Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            
            // Invertimos la lógica visual si es deuda (porque a menos meses pagados, mayor deuda)
            if ("mesesAdeudados".equals(sort)) {
                direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            }
            Sort.Order order = new Sort.Order(direction, sortField);
            if ("nombre".equals(sortField) || "direccion".equals(sortField)) {
                order = order.ignoreCase();
            }
            sortObj = Sort.by(order);
            
        } else if (sort != null && !sort.isBlank()) {
            // Es un campo calculado (como medioPago). Le decimos a la DB que NO lo ordene
            ordenarEnMemoria = true; 
        }

        // 3. Delegamos a la DB (Si era medioPago, el sortObj va vacío para que no explote)
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Cliente> clientesPage = clienteRepo.findFiltered(nombre, deudores, pageable);

        // 4. Mapeamos de Entidad a DTO
        List<ClienteResp> filtrados = new java.util.ArrayList<>(
                clientesPage.stream().map(this::castToResponse).toList()
        );

        // 5. Si era "medioPago", lo ordenamos acá en Java usando la lista que ya trajimos
        if (ordenarEnMemoria) {
            java.util.Comparator<ClienteResp> comp = null;

            if ("medioPago".equals(sort)) {
                comp = java.util.Comparator.comparing(
                    c -> c.getMedioPago() == null ? "" : c.getMedioPago(),
                    String::compareToIgnoreCase
                );
            }

            // Si encontramos un comparador válido, ordenamos la lista
            if (comp != null) {
                if ("desc".equalsIgnoreCase(dir)) {
                    comp = comp.reversed();
                }
                filtrados.sort(comp);
            }
        }

        // 6. Devolvemos la página lista para el Frontend
        return new PageImpl<>(
                filtrados,
                pageable,
                clientesPage.getTotalElements()
        );
    }
    @Transactional
    public ClienteResp findById (Long id){
       Cliente cliente = clienteRepo.findById(id)
       .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
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

        Sort sortObj = Sort.by(Sort.Order.asc("nombre").ignoreCase());

        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepo.findAll(sortObj)
                : clienteRepo.findByNombreContainingIgnoreCase(nombre, sortObj);

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
                cliente.getDeudaInstalacion(),
                cliente.getCostoInstalacion(),
                (ultimoPago != null) ? ultimoPago.getNota() : null,
                (ultimoPago != null) ? ultimoPago.getFechaPago() : null,
                (ultimoPago != null) ? ultimoPago.getMonto() : null,
                (cliente.getPlan() != null) ? cliente.getPlan().getCantidadMB() : null
        );
    }

    public void delete(Long id) {
        clienteRepo.delete(clienteRepo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado")));
    }
    @Transactional
    public void deleteAll() {
        clienteRepo.deleteAll();
    }
}
