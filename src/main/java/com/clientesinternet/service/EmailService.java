package com.clientesinternet.service;

import com.clientesinternet.entity.Configuracion;
import com.clientesinternet.repository.ConfiguracionRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailService {

    private final ConfiguracionRepository configRepo;

    public EmailService(ConfiguracionRepository configRepo) {
        this.configRepo = configRepo;
    }

    public void enviarReciboConAdjunto(String destinatario, byte[] pdfContent, String nombreCliente) throws Exception {
        
        String correoAdmin = configRepo.findById("EMAIL_ADMIN")
                .map(Configuracion::getValorString)
                .orElse("");
        String passAdmin = configRepo.findById("PASSWORD_ADMIN")
                .map(Configuracion::getValorString)
                .orElse("");

        if (correoAdmin.isEmpty() || passAdmin.isEmpty()) {
            throw new RuntimeException("El correo del sistema no esta configurado.");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(correoAdmin);
        mailSender.setPassword(passAdmin);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        helper.setTo(destinatario);
        helper.setFrom(correoAdmin, "Servicio de Internet");
        helper.setSubject("Recibo de pago de Internet");
        helper.setText("Hola " + nombreCliente + ",\n\nAdjunto encontraras el comprobante de tu ultimo pago.");
        helper.addAttachment("Recibo_Internet.pdf", new ByteArrayResource(pdfContent));

        mailSender.send(mensaje);
    }
}