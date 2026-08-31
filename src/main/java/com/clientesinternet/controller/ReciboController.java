package com.clientesinternet.controller;

import com.clientesinternet.entity.Cliente;
import com.clientesinternet.repository.ClienteRepository;
import com.clientesinternet.service.EmailService;
import com.clientesinternet.service.ReciboPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/recibos")
public class ReciboController {

    private final ReciboPdfService pdfService;
    private final EmailService emailService;
    private final ClienteRepository clienteRepository;

    public ReciboController(ReciboPdfService pdfService, EmailService emailService, ClienteRepository clienteRepository) {
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/pdf-individual/{id}")
    public ResponseEntity<byte[]> generarPdfIndividual(@PathVariable Long id) {
        try {
            byte[] pdfContent = pdfService.generarPdfIndividual(id);
            return crearPdfResponse(pdfContent, "recibo_cliente_" + id + ".pdf");
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<byte[]> crearPdfResponse(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
    @PostMapping("/enviar-email/{id}")
    public ResponseEntity<String> enviarReciboPorEmail(@PathVariable Long id) {
        try {
            // 1. Buscamos el cliente para saber su correo
            Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            if (cliente.getEmail() == null || cliente.getEmail().isEmpty()) {
                return new ResponseEntity<>("El cliente no tiene un email registrado", HttpStatus.BAD_REQUEST);
            }

            // 2. Generamos el PDF usando tu servicio existente en memoria
            byte[] pdfContent = pdfService.generarPdfIndividual(id);
            
            // 3. Enviamos el correo con el adjunto
            emailService.enviarReciboConAdjunto(cliente.getEmail(), pdfContent, cliente.getNombre());
            
            return ResponseEntity.ok("Correo enviado con éxito");
        } catch (Exception e) {
            return new ResponseEntity<>("Error al enviar correo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}