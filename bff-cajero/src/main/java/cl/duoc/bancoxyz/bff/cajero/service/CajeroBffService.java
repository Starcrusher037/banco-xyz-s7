package cl.duoc.bancoxyz.bff.cajero.service;

import cl.duoc.bancoxyz.bff.cajero.client.CoreClient;
import cl.duoc.bancoxyz.bff.cajero.dto.ConsultaSaldoCajeroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.RespuestaRetiroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.SolicitudRetiroCajeroDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class CajeroBffService {

    private static final long LIMITE_MAXIMO_RETIRO = 200000L;
    private static final long MULTIPLO_BILLETES = 5000L;

    private final CoreClient coreClient;

    public CajeroBffService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public ConsultaSaldoCajeroDto consultarSaldoCajero(Long cuentaId, String terminalId) {
        Map<String, Object> cuenta = coreClient.obtenerCuentaPorId(cuentaId);
        if (cuenta == null) {
            throw new RuntimeException("Cuenta no encontrada: " + cuentaId);
        }

        String titular = (String) cuenta.get("nombreTitular");
        Long saldo = ((Number) cuenta.get("saldo")).longValue();
        long maximoGiro = Math.min(saldo, LIMITE_MAXIMO_RETIRO);

        return new ConsultaSaldoCajeroDto(
                cuentaId,
                titular,
                saldo,
                maximoGiro,
                terminalId != null ? terminalId : "ATM-SCL-01",
                true,
                "Seleccione el monto que desea retirar."
        );
    }

    public RespuestaRetiroDto procesarRetiroCajero(Long cuentaId, SolicitudRetiroCajeroDto solicitud) {
        if (solicitud.getPin() == null || !solicitud.getPin().matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN de seguridad inválido. Debe contener 4 dígitos.");
        }

        Long monto = solicitud.getMonto();
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser mayor a $0.");
        }

        if (monto % MULTIPLO_BILLETES != 0) {
            throw new IllegalArgumentException("El cajero solo entrega billetes en múltiplos de $" + MULTIPLO_BILLETES);
        }

        if (monto > LIMITE_MAXIMO_RETIRO) {
            throw new IllegalArgumentException("El monto excede el límite máximo por giro ($" + LIMITE_MAXIMO_RETIRO + ").");
        }

        String terminal = solicitud.getTerminalId() != null ? solicitud.getTerminalId() : "ATM-SCL-01";
        String detalle = "Giro en cajero automático - Terminal: " + terminal;

        Map<String, Object> resCore = coreClient.procesarRetiro(cuentaId, monto, "CAJERO_ATM", detalle);

        Map<String, Object> cuentaActualizada = coreClient.obtenerCuentaPorId(cuentaId);
        Double saldoRemanente = ((Number) cuentaActualizada.get("saldo")).doubleValue();
        Long txId = resCore != null && resCore.get("id") != null ? ((Number) resCore.get("id")).longValue() : 5001L;

        String authCode = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new RespuestaRetiroDto(
                authCode,
                txId,
                cuentaId,
                monto.doubleValue(),
                saldoRemanente,
                true,
                "Retiro exitoso. Por favor retire su dinero y comprobante."
        );
    }
}
