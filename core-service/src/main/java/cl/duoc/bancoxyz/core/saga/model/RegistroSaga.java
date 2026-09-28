package cl.duoc.bancoxyz.core.saga.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroSaga {
    private String sagaId;
    private String tipoOperacion;
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private Long monto;
    private String canal;
    private EstadoSaga estado;
    private String mensaje;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
