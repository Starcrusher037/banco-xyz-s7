package cl.duoc.bancoxyz.core.saga.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaTransferenciaEvent implements Serializable {
    private String sagaId;
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private Long monto;
    private String canal;
    private String comentario;
    private LocalDateTime timestamp;
}
