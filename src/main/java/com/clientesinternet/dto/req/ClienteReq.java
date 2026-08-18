package com.clientesinternet.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteReq {

    @NotBlank
    private String nombre;

    private String telefono;

    private String direccion;
    private String email;
    private Boolean tieneFibraTV;
    private Boolean tieneTV;
    private String usuarioFibraTV;
    private Boolean esDemo;
    private java.time.LocalDate fechaVencimientoDemo;
    private Integer cantidadMB;
    private String dni;
    private String deudaInstalacion;
    private Integer costoInstalacion;
}
