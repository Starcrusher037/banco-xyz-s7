package cl.duoc.bancoxyz.bff.movil.service;

import cl.duoc.bancoxyz.bff.movil.client.CoreClient;
import cl.duoc.bancoxyz.bff.movil.dto.ResumenCuentaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.SolicitudTransferenciaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MovilBffService {

    private static final Logger log = LoggerFactory.getLogger(MovilBffService.class);
    private final CoreClient coreClient;

    public MovilBffService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @CircuitBreaker(name = "coreService", fallbackMethod = "fallbackObtenerResumenMovil")
    @Retry(name = "coreService")
    public ResumenCuentaMovilDto obtenerResumenMovil(Long cuentaId) {
        Map<String, Object> cuenta = coreClient.obtenerCuentaPorId(cuentaId);
        if (cuenta == null) {
            throw new RuntimeException("Cuenta no encontrada: " + cuentaId);
        }

        List<Map<String, Object>> rawTx = coreClient.obtenerTransaccionesPorCuenta(cuentaId);
        List<TransaccionMovilDto> ultimas3 = rawTx.stream()
                .limit(3)
                .map(this::mapToMovilTx)
                .collect(Collectors.toList());

        String titular = (String) cuenta.get("nombreTitular");
        Long saldo = ((Number) cuenta.get("saldo")).longValue();
        String tipo = (String) cuenta.get("tipoCuenta");

        return new ResumenCuentaMovilDto(cuentaId, titular, saldo, tipo, ultimas3, "Saldo actualizado en tiempo real.");
    }

    // Método Fallback para tolerancia a fallos en caso de caída del Core Bancario
    public ResumenCuentaMovilDto fallbackObtenerResumenMovil(Long cuentaId, Throwable ex) {
        log.warn("Circuito activado o Core no disponible para cuenta {}. Razón: {}", cuentaId, ex.getMessage());
        return new ResumenCuentaMovilDto(
                cuentaId,
                "Cliente Banco XYZ (Modo Contingencia)",
                0L,
                "CUENTA_CORRIENTE",
                Collections.emptyList(),
                "Servicio operando en modo degradado por contingencia del Core Bancario. Su saldo se sincronizará en breve."
        );
    }

    @CircuitBreaker(name = "coreService", fallbackMethod = "fallbackConsultarSaldoMovil")
    @Retry(name = "coreService")
    public Long consultarSaldoMovil(Long cuentaId) {
        Map<String, Object> cuenta = coreClient.obtenerCuentaPorId(cuentaId);
        if (cuenta == null) {
            throw new RuntimeException("Cuenta no encontrada: " + cuentaId);
        }
        return ((Number) cuenta.get("saldo")).longValue();
    }

    public Long fallbackConsultarSaldoMovil(Long cuentaId, Throwable ex) {
        log.warn("Fallback saldo ejecutado para cuenta {}. Razón: {}", cuentaId, ex.getMessage());
        return -1L; // Indica modo degradado
    }

    @CircuitBreaker(name = "coreService", fallbackMethod = "fallbackTransferirMovil")
    public TransaccionMovilDto transferirMovil(Long cuentaOrigenId, SolicitudTransferenciaMovilDto solicitud) {
        Map<String, Object> res = coreClient.procesarTransferencia(
                cuentaOrigenId,
                solicitud.getCuentaDestinoId(),
                solicitud.getMonto(),
                solicitud.getComentario()
        );
        return mapToMovilTx(res);
    }

    public TransaccionMovilDto fallbackTransferirMovil(Long cuentaOrigenId, SolicitudTransferenciaMovilDto solicitud, Throwable ex) {
        log.error("Error al procesar transferencia móvil: {}", ex.getMessage());
        throw new RuntimeException("El servicio de transferencias se encuentra temporalmente no disponible (Circuito abierto). Intente más tarde.");
    }

    private TransaccionMovilDto mapToMovilTx(Map<String, Object> tx) {
        Long id = ((Number) tx.get("id")).longValue();
        String fecha = (String) tx.get("fecha");
        Long monto = ((Number) tx.get("monto")).longValue();
        String tipo = (String) tx.get("tipo");
        String descripcion = (String) tx.get("descripcion");
        return new TransaccionMovilDto(id, fecha, monto, tipo, descripcion);
    }
}
