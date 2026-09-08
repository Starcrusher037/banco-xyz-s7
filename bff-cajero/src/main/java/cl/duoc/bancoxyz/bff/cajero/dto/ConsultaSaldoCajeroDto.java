package cl.duoc.bancoxyz.bff.cajero.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de saldo adaptada para terminales de cajero automático (ATM)")
public class ConsultaSaldoCajeroDto {

    @Schema(description = "ID de la cuenta", example = "101")
    private Long cuentaId;

    @Schema(description = "Nombre del titular para visualización en pantalla de ATM", example = "Carlos Soto")
    private String nombreTitular;

    @Schema(description = "Saldo disponible para retiro en pesos", example = "150000")
    private Long saldoDisponible;

    @Schema(description = "Límite máximo permitido por transacción en cajero ($200.000)", example = "200000")
    private Long limiteGiroMaximo;

    @Schema(description = "Código del cajero automático consultante", example = "ATM-SCL-01")
    private String terminalId;

    @Schema(description = "Indica si la cuenta está habilitada para operar en cajero", example = "true")
    private Boolean operacionPermitida;

    @Schema(description = "Mensaje instructivo para la pantalla del cajero", example = "Seleccione el monto que desea retirar")
    private String mensajePantalla;

    public ConsultaSaldoCajeroDto() {
    }

    public ConsultaSaldoCajeroDto(Long cuentaId, String nombreTitular, Long saldoDisponible, Long limiteGiroMaximo, String terminalId, Boolean operacionPermitida, String mensajePantalla) {
        this.cuentaId = cuentaId;
        this.nombreTitular = nombreTitular;
        this.saldoDisponible = saldoDisponible;
        this.limiteGiroMaximo = limiteGiroMaximo;
        this.terminalId = terminalId;
        this.operacionPermitida = operacionPermitida;
        this.mensajePantalla = mensajePantalla;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(String nombreTitular) {
        this.nombreTitular = nombreTitular;
    }

    public Long getSaldoDisponible() {
        return saldoDisponible;
    }

    public void setSaldoDisponible(Long saldoDisponible) {
        this.saldoDisponible = saldoDisponible;
    }

    public Long getLimiteGiroMaximo() {
        return limiteGiroMaximo;
    }

    public void setLimiteGiroMaximo(Long limiteGiroMaximo) {
        this.limiteGiroMaximo = limiteGiroMaximo;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Boolean getOperacionPermitida() {
        return operacionPermitida;
    }

    public void setOperacionPermitida(Boolean operacionPermitida) {
        this.operacionPermitida = operacionPermitida;
    }

    public String getMensajePantalla() {
        return mensajePantalla;
    }

    public void setMensajePantalla(String mensajePantalla) {
        this.mensajePantalla = mensajePantalla;
    }
}
