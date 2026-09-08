package cl.duoc.bancoxyz.bff.movil.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representación mínima de un movimiento bancario para la app móvil")
public class TransaccionMovilDto {

    @Schema(description = "ID de la transacción", example = "5001")
    private Long id;

    @Schema(description = "Fecha de la operación", example = "2024-03-01")
    private String fecha;

    @Schema(description = "Monto en pesos (positivo para abono, negativo para cargo)", example = "-15000")
    private Long monto;

    @Schema(description = "Tipo de transacción", example = "debito")
    private String tipo;

    @Schema(description = "Glosa breve del movimiento", example = "Supermercado")
    private String descripcion;

    public TransaccionMovilDto() {
    }

    public TransaccionMovilDto(Long id, String fecha, Long monto, String tipo, String descripcion) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Long getMonto() {
        return monto;
    }

    public void setMonto(Long monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
