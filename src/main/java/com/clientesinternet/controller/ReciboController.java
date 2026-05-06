package com.clientesinternet.controller;

import com.clientesinternet.service.ReciboPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // 'inline' abre el PDF en el navegador.
            headers.setContentDispositionFormData("inline", "recibos_masivos.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}