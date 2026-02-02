package com.clientesinternet.dto.req;

import com.clientesinternet.entity.MedioPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoReq {

    @NotNull
    @Positive
    private BigDecimal monto;

    @NotNull
    private MedioPago medioPago;

    private String nota;
    private String dniPagador;

}
