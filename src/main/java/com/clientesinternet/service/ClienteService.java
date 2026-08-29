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

import com.clientesinternet.repository.PlanInternetRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;


@Service("ClienteService")
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private PlanInternetRepository planRepo;

    public ClienteService(ClienteRepository clienteRepo, PlanInternetRepository planRepo) {
        this.clienteRepo = clienteRepo;
        this.planRepo = planRepo;
    }

    @Transactional
    public ClienteResp save(ClienteReq req) {
        PlanInternet plan = null;
        if (req.getCantidadMB() != null) {
            plan = planRepo.findById(req.getCantidadMB().longValue()).orElse(null);
        }

        Cliente cliente = Cliente.builder()
                .nombre(req.getNombre())
                .telefono(req.getTelefono())
                .direccion(req.getDireccion())
                .email(req.getEmail())
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
        cliente.setEmail(req.getEmail());
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
        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepo.findAll()
                : clienteRepo.findByNombreContainingIgnoreCase(nombre, Sort.by("nombre").ascending());

        List<ClienteResp> filtrados = clientes.stream()
                .map(this::castToResponse)
                .filter(cliente -> deudores == null || cliente.isTieneDeuda() == deudores)
                .sorted(comparador(sort, dir))
                .toList();

        int inicio = Math.min(page * size, filtrados.size());
        int fin = Math.min(inicio + size, filtrados.size());
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(filtrados.subList(inicio, fin), pageable, filtrados.size());
    }
    @Transactional
    public ClienteResp findById (Long id){
       Cliente cliente = clienteRepo.findById(id)
       .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return castToResponse(cliente);
    }

    @Transactional
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
        List<Pago> pagos = cliente.getPagos();
        Pago ultimoPago = (pagos != null && !pagos.isEmpty()) 
                ? pagos.stream()
                    .max(Comparator.comparing(Pago::getId))
                    .orElse(null) 
                : null;

        int mesesAdeudados = cliente.calcularMesesAdeudados();
        boolean deuda = mesesAdeudados > 0;

        String dni = (cliente.getDni() != null && !cliente.getDni().isBlank()) 
                ? cliente.getDni() 
                : ((ultimoPago != null && (ultimoPago.getMedioPago() == MedioPago.TRANSFERENCIA 
                    || ultimoPago.getMedioPago() == MedioPago.TARJETA)) 
                    ? ultimoPago.getDniPagador() : null);

        String medioPago = (ultimoPago != null) ? ultimoPago.getMedioPago().name() : null;
        int mesesPagados = (cliente.getMesesPagados() != null) ? cliente.getMesesPagados() : 0;
        boolean tieneFibraTV = (cliente.getTieneFibraTV() != null) ? cliente.getTieneFibraTV() : false;
        boolean tieneTV = (cliente.getTieneTV() != null) ? cliente.getTieneTV() : false;
        boolean esDemo = (cliente.getEsDemo() != null) ? cliente.getEsDemo() : false;

        return new ClienteResp(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getEmail(),
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

    private Comparator<ClienteResp> comparador(String sort, String dir) {
        Comparator<ClienteResp> comparador = switch (sort == null ? "nombre" : sort) {
            case "mesesAdeudados" -> Comparator.comparingInt(ClienteResp::getMesesAdeudados);
            case "mesesPagados" -> Comparator.comparingInt(ClienteResp::getMesesPagados);
            case "medioPago" -> Comparator.comparing(
                    c -> c.getMedioPago() == null ? "" : c.getMedioPago(),
                    String::compareToIgnoreCase);
            default -> Comparator.comparing(
                    c -> c.getNombre() == null ? "" : c.getNombre(),
                    String::compareToIgnoreCase);
        };
        return "desc".equalsIgnoreCase(dir) ? comparador.reversed() : comparador;
    }

    public void delete(Long id) {
        clienteRepo.delete(clienteRepo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado")));
    }
    @Transactional
    public void deleteAll() {
        clienteRepo.deleteAll();
    }
}
