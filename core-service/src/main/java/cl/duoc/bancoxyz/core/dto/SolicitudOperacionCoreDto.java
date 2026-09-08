package cl.duoc.bancoxyz.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudOperacionCoreDto {
    private Long cuentaId;
    private Long monto;
    private String canal;
    private String detalle;
    private Long cuentaDestinoId;
}
