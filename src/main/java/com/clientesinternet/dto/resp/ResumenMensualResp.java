package com.clientesinternet.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMensualResp {
    private BigDecimal totalMesEfectivo = BigDecimal.ZERO;
    private BigDecimal totalMesDigital = BigDecimal.ZERO; 
    private BigDecimal totalMesGeneral = BigDecimal.ZERO;
    private List<ResumenDiaDTO> dias = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor 
    public static class ResumenDiaDTO {
        private String fecha; // YYYY-MM-DD
        private BigDecimal efectivo = BigDecimal.ZERO;
        private BigDecimal digital = BigDecimal.ZERO;
        private BigDecimal totalDia = BigDecimal.ZERO;
    }
}