package com.clientesinternet.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResp {
    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
    private String email;
    private boolean tieneDeuda;
    private int mesesAdeudados;
    private int mesesPagados;
    private boolean tieneFibraTV;
    private String usuarioFibraTV;
    private Boolean tieneTV;
    private boolean esDemo;
    private LocalDate fechaVencimientoDemo;
    private String medioPago; 
    private String dni;
    private String deudaInstalacion;
    private Integer costoInstalacion;       
    private String nota;      
    private LocalDate fechaUltimoPago;
    private BigDecimal montoUltimoPago;
    private Integer cantidadMB;
}