package cl.duoc.bancoxyz.bff.movil.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "DTO ultracompacto para la aplicación móvil")
public class ResumenCuentaMovilDto {

    @Schema(description = "Número identificador de la cuenta", example = "101")
    private Long numeroCuenta;

    @Schema(description = "Nombre abreviado del titular", example = "Carlos Soto")
    private String titular;

    @Schema(description = "Saldo disponible actual en pesos chilenos", example = "1500000")
    private Long saldoDisponible;

    @Schema(description = "Tipo de producto financiero", example = "cuenta_corriente")
    private String tipoCuenta;

    @Schema(description = "Últimos movimientos más recientes")
    private List<TransaccionMovilDto> ultimosMovimientos;

    @Schema(description = "Mensaje contextual para el usuario", example = "Saldo actualizado correctamente")
    private String mensajeInformativo;

    public ResumenCuentaMovilDto() {
    }

    public ResumenCuentaMovilDto(Long numeroCuenta, String titular, Long saldoDisponible, String tipoCuenta, List<TransaccionMovilDto> ultimosMovimientos, String mensajeInformativo) {
        this.numeroCuenta = numeroCuenta;
        this.titular = titular;
        this.saldoDisponible = saldoDisponible;
        this.tipoCuenta = tipoCuenta;
        this.ultimosMovimientos = ultimosMovimientos;
        this.mensajeInformativo = mensajeInformativo;
    }

    public Long getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(Long numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public Long getSaldoDisponible() {
        return saldoDisponible;
    }

    public void setSaldoDisponible(Long saldoDisponible) {
        this.saldoDisponible = saldoDisponible;
    }

    public String getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public List<TransaccionMovilDto> getUltimosMovimientos() {
        return ultimosMovimientos;
    }

    public void setUltimosMovimientos(List<TransaccionMovilDto> ultimosMovimientos) {
        this.ultimosMovimientos = ultimosMovimientos;
    }

    public String getMensajeInformativo() {
        return mensajeInformativo;
    }

    public void setMensajeInformativo(String mensajeInformativo) {
        this.mensajeInformativo = mensajeInformativo;
    }
}
