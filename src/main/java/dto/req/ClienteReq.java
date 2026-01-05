package dto.req;

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
}
