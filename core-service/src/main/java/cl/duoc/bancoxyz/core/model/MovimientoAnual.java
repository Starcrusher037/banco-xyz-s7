package cl.duoc.bancoxyz.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoAnual {
    private Long cuentaId;
    private String fecha;
    private String tipoTransaccion;
    private Long monto;
    private String descripcion;
}
