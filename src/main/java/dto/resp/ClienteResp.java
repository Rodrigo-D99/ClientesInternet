package dto.resp;

import lombok.*;

@Getter
@AllArgsConstructor
public class ClienteResp {

    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
    private boolean tieneDeuda;
    private Integer mesesAdeudados;
    private String nota;
}
