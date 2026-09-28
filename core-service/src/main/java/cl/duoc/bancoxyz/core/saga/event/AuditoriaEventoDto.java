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
public class AuditoriaEventoDto implements Serializable {
    private String eventoId;
    private String sagaId;
    private String tipoEvento;
    private String detalle;
    private LocalDateTime timestamp;
}
