package com.clientesinternet.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoHistorialResp {
    private LocalDate fechaPago;
    private BigDecimal monto;
    private String medioPago;
    private Integer cantidadMeses;
    private String nota;
}