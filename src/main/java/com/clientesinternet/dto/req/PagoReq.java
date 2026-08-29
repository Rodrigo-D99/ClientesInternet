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

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull
    private MedioPago medioPago;

    @Builder.Default
    @Positive
    private Integer cantidadMeses = 1;

    private String nota;
    private String dniPagador;
    private Boolean saldaInstalacion;
}
