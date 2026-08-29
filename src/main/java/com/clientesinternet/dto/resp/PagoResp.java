package com.clientesinternet.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoResp {
    private Long pagoId;
    private BigDecimal montoPagado;
    private BigDecimal precioMensualBase;
    private BigDecimal deudaAnteriorTotal;
    private BigDecimal saldoPendiente;
    private Integer mesesDeudaRestantes;
    private String advertencia;
}