package cl.duoc.bancoxyz.bff.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalle extendido de una transacción para la plataforma Web")
public class TransaccionWebDto {

    @Schema(description = "Identificador de la transacción", example = "5001")
    private Long id;

    @Schema(description = "Fecha de emisión", example = "2024-03-01")
    private String fecha;

    @Schema(description = "Monto de la transacción", example = "15000")
    private Long monto;

    @Schema(description = "Tipo de operación", example = "debito")
    private String tipo;

    @Schema(description = "Detalle descriptivo", example = "Compra en Supermercado Líder")
    private String descripcion;

    @Schema(description = "Canal en que se originó el movimiento", example = "POS-Fisico")
    private String canalOrigen;

    @Schema(description = "Categoría analítica para reportes web", example = "Alimentación")
    private String categoria;

    public TransaccionWebDto() {
    }

    public TransaccionWebDto(Long id, String fecha, Long monto, String tipo, String descripcion, String canalOrigen, String categoria) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.canalOrigen = canalOrigen;
        this.categoria = categoria;
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

    public String getCanalOrigen() {
        return canalOrigen;
    }

    public void setCanalOrigen(String canalOrigen) {
        this.canalOrigen = canalOrigen;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
