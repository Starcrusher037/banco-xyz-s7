package cl.duoc.bancoxyz.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {
    private Long id;
    private Long cuentaId;
    private String fecha;
    private Long monto;
    private String tipo;
    private String descripcion;
    private String canal;
}
