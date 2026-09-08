package cl.duoc.bancoxyz.bff.cajero.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comprobante y autorización de dispensación de efectivo en cajero")
public class RespuestaRetiroDto {

    @Schema(description = "Código de autorización para el dispensador de billetes", example = "AUTH-ATM-982145")
    private String codigoAutorizacion;

    @Schema(description = "ID único de la transacción", example = "5001")
    private Long transaccionId;

    @Schema(description = "Número de cuenta", example = "101")
    private Long cuentaId;

    @Schema(description = "Monto retirado", example = "40000.0")
    private Double montoRetirado;

    @Schema(description = "Nuevo saldo remanente", example = "30000.0")
    private Double nuevoSaldo;

    @Schema(description = "Indicador de éxito para liberar billetes en hardware", example = "true")
    private Boolean dispensacionEfectivoPermitida;

    @Schema(description = "Mensaje impreso en el comprobante del cajero", example = "Retiro exitoso. Por favor retire su dinero y comprobante.")
    private String mensajeRecibo;

    public RespuestaRetiroDto() {}

    public RespuestaRetiroDto(String codigoAutorizacion, Long transaccionId, Long cuentaId, Double montoRetirado, Double nuevoSaldo, Boolean dispensacionEfectivoPermitida, String mensajeRecibo) {
        this.codigoAutorizacion = codigoAutorizacion;
        this.transaccionId = transaccionId;
        this.cuentaId = cuentaId;
        this.montoRetirado = montoRetirado;
        this.nuevoSaldo = nuevoSaldo;
        this.dispensacionEfectivoPermitida = dispensacionEfectivoPermitida;
        this.mensajeRecibo = mensajeRecibo;
    }

    public RespuestaRetiroDto(Long cuentaId, Long montoRetirado, Long nuevoSaldo, String codigoAutorizacion, Long transaccionId, Boolean dispensacionEfectivoPermitida, String mensajeRecibo) {
        this.cuentaId = cuentaId;
        this.montoRetirado = montoRetirado != null ? montoRetirado.doubleValue() : null;
        this.nuevoSaldo = nuevoSaldo != null ? nuevoSaldo.doubleValue() : null;
        this.codigoAutorizacion = codigoAutorizacion;
        this.transaccionId = transaccionId;
        this.dispensacionEfectivoPermitida = dispensacionEfectivoPermitida;
        this.mensajeRecibo = mensajeRecibo;
    }

    public static RespuestaRetiroDtoBuilder builder() {
        return new RespuestaRetiroDtoBuilder();
        }

    public String getCodigoAutorizacion() {
         return codigoAutorizacion; 
        }
    public void setCodigoAutorizacion(String codigoAutorizacion) {
         this.codigoAutorizacion = codigoAutorizacion; 
        }
    public Long getTransaccionId() {
         return transaccionId; 
        }
    public void setTransaccionId(Long transaccionId) { 
        this.transaccionId = transaccionId; 
        }
    public Long getCuentaId() {
         return cuentaId; 
        }
    public void setCuentaId(Long cuentaId)  {
         this.cuentaId = cuentaId;         
        }
    public Double getMontoRetirado() { 
        return montoRetirado; 
        }
    public void setMontoRetirado(Double montoRetirado) {
         this.montoRetirado = montoRetirado; }
    public Double getNuevoSaldo() { 
        return nuevoSaldo; 
        }
    public void setNuevoSaldo(Double nuevoSaldo) {
         this.nuevoSaldo = nuevoSaldo; 
        }
    public Boolean getDispensacionEfectivoPermitida() { return dispensacionEfectivoPermitida; 

        }
    public void setDispensacionEfectivoPermitida(Boolean dispensacionEfectivoPermitida) {
         this.dispensacionEfectivoPermitida = dispensacionEfectivoPermitida; 
        }
    public String getMensajeRecibo() { 
        return mensajeRecibo; 
        }
    public void setMensajeRecibo(String mensajeRecibo) {
         this.mensajeRecibo = mensajeRecibo; 
        }

    public static class RespuestaRetiroDtoBuilder {
        private String codigoAutorizacion;
        private Long transaccionId;
        private Long cuentaId;
        private Double montoRetirado;
        private Double nuevoSaldo;
        private Boolean dispensacionEfectivoPermitida;
        private String mensajeRecibo;

        public RespuestaRetiroDtoBuilder codigoAutorizacion(String codigoAutorizacion) { this.codigoAutorizacion = codigoAutorizacion; return this; }
        public RespuestaRetiroDtoBuilder transaccionId(Long transaccionId) { this.transaccionId = transaccionId; return this; }
        public RespuestaRetiroDtoBuilder cuentaId(Long cuentaId) { this.cuentaId = cuentaId; return this; }
        public RespuestaRetiroDtoBuilder montoRetirado(Double montoRetirado) { this.montoRetirado = montoRetirado; return this; }
        public RespuestaRetiroDtoBuilder nuevoSaldo(Double nuevoSaldo) { this.nuevoSaldo = nuevoSaldo; return this; }
        public RespuestaRetiroDtoBuilder dispensacionEfectivoPermitida(Boolean dispensacionEfectivoPermitida) { this.dispensacionEfectivoPermitida = dispensacionEfectivoPermitida; return this; }
        public RespuestaRetiroDtoBuilder mensajeRecibo(String mensajeRecibo) { this.mensajeRecibo = mensajeRecibo; return this; }

        public RespuestaRetiroDto build() {
            return new RespuestaRetiroDto(codigoAutorizacion, transaccionId, cuentaId, montoRetirado, nuevoSaldo, dispensacionEfectivoPermitida, mensajeRecibo);
        }
    }
}
