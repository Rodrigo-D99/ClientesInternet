package com.clientesinternet.dto.resp;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoHistorialResp(
        Long id,
        LocalDate fechaPago,
        BigDecimal monto,
        String medioPago,
        Integer cantidadMeses,
        String nota
) {}