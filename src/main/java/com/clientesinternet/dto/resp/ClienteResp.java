package com.clientesinternet.dto.resp;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ClienteResp {
    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
    private boolean tieneDeuda;
    private int mesesAdeudados;
    private String medioPago; // EFECTIVO, TRANSFERENCIA, TARJETA
    private String dni;       // solo si medioPago es transferencia/tarjeta
    private String nota;      // monto pagado o comentario
    private LocalDate fechaUltimoPago;
    private BigDecimal montoUltimoPago;
}
