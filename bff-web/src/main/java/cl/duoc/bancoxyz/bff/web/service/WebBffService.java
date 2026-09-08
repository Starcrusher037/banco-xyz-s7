package cl.duoc.bancoxyz.bff.web.service;

import cl.duoc.bancoxyz.bff.web.client.CoreClient;
import cl.duoc.bancoxyz.bff.web.dto.DashboardWebDto;
import cl.duoc.bancoxyz.bff.web.dto.DetalleCuentaWebDto;
import cl.duoc.bancoxyz.bff.web.dto.TransaccionWebDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebBffService {

    private final CoreClient coreClient;

    public WebBffService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public DetalleCuentaWebDto obtenerDetalleWeb(Long cuentaId) {
        Map<String, Object> cuenta = coreClient.obtenerCuentaPorId(cuentaId);
        if (cuenta == null) {
            throw new RuntimeException("Cuenta no encontrada: " + cuentaId);
        }

        List<Map<String, Object>> rawTx = coreClient.obtenerTransaccionesPorCuenta(cuentaId);
        List<TransaccionWebDto> txWeb = rawTx.stream()
                .map(this::mapToWebTx)
                .collect(Collectors.toList());

        List<Map<String, Object>> movimientosAnuales = coreClient.obtenerMovimientosAnuales(cuentaId);

        String titular = (String) cuenta.get("nombreTitular");
        Long saldo = ((Number) cuenta.get("saldo")).longValue();
        Integer edad = ((Number) cuenta.get("edad")).intValue();
        String tipo = (String) cuenta.get("tipoCuenta");
        Long lineaSobregiro = ((Number) cuenta.get("lineaSobregiro")).longValue();
        Double tasaInteres = ((Number) cuenta.get("tasaInteresAnual")).doubleValue();
        String estado = (String) cuenta.get("estado");

        Double interesProyectado = (double) (saldo * (tasaInteres / 100.0));
        Long saldoTotalConSobregiro = saldo + lineaSobregiro;

        return new DetalleCuentaWebDto(
                cuentaId,
                titular,
                saldo,
                lineaSobregiro,
                saldoTotalConSobregiro,
                edad,
                tipo,
                tasaInteres,
                interesProyectado,
                estado,
                txWeb,
                movimientosAnuales
        );
    }

    public List<TransaccionWebDto> listarTodasTransaccionesWeb(Long cuentaId) {
        List<Map<String, Object>> rawTx = coreClient.obtenerTransaccionesPorCuenta(cuentaId);
        return rawTx.stream()
                .map(this::mapToWebTx)
                .collect(Collectors.toList());
    }

    public DashboardWebDto obtenerDashboardWeb() {
        List<Map<String, Object>> cuentas = coreClient.obtenerTodasLasCuentas();

        int totalCuentas = cuentas.size();
        long capitalTotal = cuentas.stream()
                .mapToLong(c -> ((Number) c.get("saldo")).longValue())
                .sum();
        long saldoPromedio = totalCuentas > 0 ? capitalTotal / totalCuentas : 0L;

        Map<String, Long> distribucion = cuentas.stream()
                .collect(Collectors.groupingBy(
                        c -> (String) c.get("tipoCuenta"),
                        Collectors.counting()
                ));

        return new DashboardWebDto(totalCuentas, capitalTotal, saldoPromedio, distribucion);
    }

    private TransaccionWebDto mapToWebTx(Map<String, Object> tx) {
        Long id = ((Number) tx.get("id")).longValue();
        String fecha = (String) tx.get("fecha");
        Long monto = ((Number) tx.get("monto")).longValue();
        String tipo = (String) tx.get("tipo");
        String descripcion = (String) tx.get("descripcion");
        String canal = (String) tx.get("canal");

        String categoria = "debito".equalsIgnoreCase(tipo) ? "GASTO" : "INGRESO";
        return new TransaccionWebDto(id, fecha, monto, tipo, descripcion, canal, categoria);
    }
}
