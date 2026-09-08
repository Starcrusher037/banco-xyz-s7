package cl.duoc.bancoxyz.bff.movil.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cuerpo de solicitud para realizar transferencias desde la app móvil")
public class SolicitudTransferenciaMovilDto {

    @Schema(description = "ID de la cuenta destinataria", example = "102", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cuentaDestinoId;

    @Schema(description = "Monto a transferir en pesos chilenos", example = "25000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long monto;

    @Schema(description = "Comentario o asunto opcional de la transferencia", example = "Pago almuerzo")
    private String comentario;

    public SolicitudTransferenciaMovilDto() {
    }

    public SolicitudTransferenciaMovilDto(Long cuentaDestinoId, Long monto, String comentario) {
        this.cuentaDestinoId = cuentaDestinoId;
        this.monto = monto;
        this.comentario = comentario;
    }

    public Long getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public void setCuentaDestinoId(Long cuentaDestinoId) {
        this.cuentaDestinoId = cuentaDestinoId;
    }

    public Long getMonto() {
        return monto;
    }

    public void setMonto(Long monto) {
        this.monto = monto;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
