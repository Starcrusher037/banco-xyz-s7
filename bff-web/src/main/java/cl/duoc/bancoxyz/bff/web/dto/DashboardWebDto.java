package cl.duoc.bancoxyz.bff.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Métricas globales para el panel administrativo web")
public class DashboardWebDto {

    @Schema(description = "Total de cuentas activas en el sistema", example = "150")
    private Integer totalCuentas;

    @Schema(description = "Capital total bajo custodia en pesos", example = "750000000")
    private Long capitalTotalCustodiado;

    @Schema(description = "Saldo promedio por cuenta", example = "5000000")
    private Long saldoPromedio;

    @Schema(description = "Distribución de cuentas por tipo de producto")
    private Map<String, Long> distribucionPorTipo;

    public DashboardWebDto() {
    }

    public DashboardWebDto(Integer totalCuentas, Long capitalTotalCustodiado, Long saldoPromedio, Map<String, Long> distribucionPorTipo) {
        this.totalCuentas = totalCuentas;
        this.capitalTotalCustodiado = capitalTotalCustodiado;
        this.saldoPromedio = saldoPromedio;
        this.distribucionPorTipo = distribucionPorTipo;
    }

    public Integer getTotalCuentas() {
        return totalCuentas;
    }

    public void setTotalCuentas(Integer totalCuentas) {
        this.totalCuentas = totalCuentas;
    }

    public Long getCapitalTotalCustodiado() {
        return capitalTotalCustodiado;
    }

    public void setCapitalTotalCustodiado(Long capitalTotalCustodiado) {
        this.capitalTotalCustodiado = capitalTotalCustodiado;
    }

    public Long getSaldoPromedio() {
        return saldoPromedio;
    }

    public void setSaldoPromedio(Long saldoPromedio) {
        this.saldoPromedio = saldoPromedio;
    }

    public Map<String, Long> getDistribucionPorTipo() {
        return distribucionPorTipo;
    }

    public void setDistribucionPorTipo(Map<String, Long> distribucionPorTipo) {
        this.distribucionPorTipo = distribucionPorTipo;
    }
}
