package cl.duoc.bancoxyz.bff.cajero.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de giro de dinero en cajero automático (ATM)")
public class SolicitudRetiroCajeroDto {

    @Schema(description = "Monto en pesos a retirar (múltiplo de $5.000)", example = "40000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long monto;

    @Schema(description = "PIN de seguridad de 4 dígitos", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Schema(description = "Identificador del terminal ATM", example = "ATM-SCL-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalId;

    public SolicitudRetiroCajeroDto() {
    }

    public SolicitudRetiroCajeroDto(Long monto, String pin, String terminalId) {
        this.monto = monto;
        this.pin = pin;
        this.terminalId = terminalId;
    }

    public Long getMonto() {
        return monto;
    }

    public void setMonto(Long monto) {
        this.monto = monto;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }
}
