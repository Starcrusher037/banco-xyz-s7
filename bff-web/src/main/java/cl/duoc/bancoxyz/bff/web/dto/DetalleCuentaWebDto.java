package cl.duoc.bancoxyz.bff.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "DTO enriquecido y completo para el portal web de clientes")
public class DetalleCuentaWebDto {

    @Schema(description = "ID de la cuenta", example = "101")
    private Long cuentaId;

    @Schema(description = "Nombre completo del titular", example = "Carlos Soto Pérez")
    private String nombreTitular;

    @Schema(description = "Edad del cliente", example = "35")
    private Integer edadTitular;

    @Schema(description = "Tipo de cuenta bancaria", example = "cuenta_corriente")
    private String tipoCuenta;

    @Schema(description = "Saldo contable en cuenta", example = "1500000")
    private Long saldoContable;

    @Schema(description = "Línea de sobregiro asignada", example = "300000")
    private Long lineaSobregiro;

    @Schema(description = "Saldo total disponible considerando sobregiro", example = "1800000")
    private Long saldoTotalDisponible;

    @Schema(description = "Tasa de interés anual en porcentaje", example = "3.8")
    private Double tasaInteresAnual;

    @Schema(description = "Interés mensual estimado proyectado", example = "4750.0")
    private Double interesMensualEstimado;

    @Schema(description = "Estado actual de la cuenta", example = "ACTIVA")
    private String estadoCuenta;

    @Schema(description = "Total histórico de movimientos", example = "45")
    private Integer totalTransacciones;

    @Schema(description = "Historial completo de transacciones categorizadas")
    private List<TransaccionWebDto> historialTransacciones;

    @Schema(description = "Movimientos históricos del ejercicio anual")
    private List<Map<String, Object>> historialAnual;

    public DetalleCuentaWebDto() {
    }

    public DetalleCuentaWebDto(Long cuentaId, String nombreTitular, Long saldoContable, Long lineaSobregiro, Long saldoTotalDisponible, Integer edadTitular, String tipoCuenta, Double tasaInteresAnual, Double interesMensualEstimado, String estadoCuenta, List<TransaccionWebDto> historialTransacciones, List<Map<String, Object>> historialAnual) {
        this.cuentaId = cuentaId;
        this.nombreTitular = nombreTitular;
        this.saldoContable = saldoContable;
        this.lineaSobregiro = lineaSobregiro;
        this.saldoTotalDisponible = saldoTotalDisponible;
        this.edadTitular = edadTitular;
        this.tipoCuenta = tipoCuenta;
        this.tasaInteresAnual = tasaInteresAnual;
        this.interesMensualEstimado = interesMensualEstimado;
        this.estadoCuenta = estadoCuenta;
        this.historialTransacciones = historialTransacciones;
        this.historialAnual = historialAnual;
        this.totalTransacciones = historialTransacciones != null ? historialTransacciones.size() : 0;
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

    public Integer getEdadTitular() {
        return edadTitular;
    }

    public void setEdadTitular(Integer edadTitular) {
        this.edadTitular = edadTitular;
    }

    public String getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public Long getSaldoContable() {
        return saldoContable;
    }

    public void setSaldoContable(Long saldoContable) {
        this.saldoContable = saldoContable;
    }

    public Long getLineaSobregiro() {
        return lineaSobregiro;
    }

    public void setLineaSobregiro(Long lineaSobregiro) {
        this.lineaSobregiro = lineaSobregiro;
    }

    public Long getSaldoTotalDisponible() {
        return saldoTotalDisponible;
    }

    public void setSaldoTotalDisponible(Long saldoTotalDisponible) {
        this.saldoTotalDisponible = saldoTotalDisponible;
    }

    public Double getTasaInteresAnual() {
        return tasaInteresAnual;
    }

    public void setTasaInteresAnual(Double tasaInteresAnual) {
        this.tasaInteresAnual = tasaInteresAnual;
    }

    public Double getInteresMensualEstimado() {
        return interesMensualEstimado;
    }

    public void setInteresMensualEstimado(Double interesMensualEstimado) {
        this.interesMensualEstimado = interesMensualEstimado;
    }

    public String getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(String estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public Integer getTotalTransacciones() {
        return totalTransacciones;
    }

    public void setTotalTransacciones(Integer totalTransacciones) {
        this.totalTransacciones = totalTransacciones;
    }

    public List<TransaccionWebDto> getHistorialTransacciones() {
        return historialTransacciones;
    }

    public void setHistorialTransacciones(List<TransaccionWebDto> historialTransacciones) {
        this.historialTransacciones = historialTransacciones;
    }

    public List<Map<String, Object>> getHistorialAnual() {
        return historialAnual;
    }

    public void setHistorialAnual(List<Map<String, Object>> historialAnual) {
        this.historialAnual = historialAnual;
    }
}
