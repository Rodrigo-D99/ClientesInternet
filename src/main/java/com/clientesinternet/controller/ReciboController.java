package com.clientesinternet.controller;

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

    public ReciboController(ReciboPdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/pdf-masivo")
    public ResponseEntity<byte[]> generarPdfMasivo() {
        try {
            byte[] pdfContent = pdfService.generarPdfMasivo();
            return crearPdfResponse(pdfContent, "recibos_masivos.pdf");
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            // 1. Generar el PDF usando tu servicio existente
            byte[] pdfContent = pdfService.generarPdfIndividual(id);
            
            // 2. Aquí llamarías a un nuevo EmailService (que debes crear)
            // emailService.enviarReciboConAdjunto(cliente.getEmail(), pdfContent);
            
            return ResponseEntity.ok("Correo enviado con éxito");
        } catch (Exception e) {
            return new ResponseEntity<>("Error al enviar correo", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}