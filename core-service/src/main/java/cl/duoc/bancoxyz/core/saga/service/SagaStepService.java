package cl.duoc.bancoxyz.core.saga.service;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.Transaccion;
import cl.duoc.bancoxyz.core.repository.BancoRepository;
import cl.duoc.bancoxyz.core.saga.model.EstadoSaga;
import cl.duoc.bancoxyz.core.saga.model.RegistroSaga;
import cl.duoc.bancoxyz.core.saga.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class SagaStepService {

    private static final Logger log = LoggerFactory.getLogger(SagaStepService.class);

    private final BancoRepository bancoRepository;
    private final SagaRepository sagaRepository;

    public SagaStepService(BancoRepository bancoRepository, SagaRepository sagaRepository) {
        this.bancoRepository = bancoRepository;
        this.sagaRepository = sagaRepository;
    }

    public synchronized void ejecutarDebitoInicial(String sagaId, Long cuentaOrigenId, Long monto, String canal) {
        Cuenta origen = bancoRepository.buscarCuentaPorId(cuentaOrigenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de origen " + cuentaOrigenId + " no existe"));

        long disponible = origen.getSaldoContable() + origen.getLineaSobregiro();
        if (monto > disponible) {
            throw new IllegalArgumentException("Saldo insuficiente en cuenta origen " + cuentaOrigenId);
        }

        // Paso 1: Débito local
        origen.setSaldoContable(origen.getSaldoContable() - monto);
        bancoRepository.guardarCuenta(origen);

        Transaccion cargo = new Transaccion(
                null,
                cuentaOrigenId,
                LocalDate.now(),
                monto,
                "debito",
                "[SAGA " + sagaId.substring(0, 8) + "] Débito transferencia en proceso hacia destino",
                canal
        );
        bancoRepository.guardarTransaccion(cargo);

        // Actualizar saga
        sagaRepository.buscarPorSagaId(sagaId).ifPresent(saga -> {
            saga.setEstado(EstadoSaga.DEBITO_COMPLETADO);
            saga.setFechaActualizacion(LocalDateTime.now());
            saga.setMensaje("Débito completado en cuenta origen. Esperando acreditación destino.");
            sagaRepository.guardarSaga(saga);
        });

        log.info("[SAGA-PASO-1] Débito exitoso en cuenta origen {}. Nuevo saldo: {}", cuentaOrigenId, origen.getSaldoContable());
    }

    public synchronized void ejecutarAbonoDestino(String sagaId, Long cuentaDestinoId, Long monto, String canal, String comentario) {
        Cuenta destino = bancoRepository.buscarCuentaPorId(cuentaDestinoId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino " + cuentaDestinoId + " no existe"));

        if ("inactiva".equalsIgnoreCase(destino.getEstado()) || "bloqueada".equalsIgnoreCase(destino.getEstado())) {
            throw new IllegalStateException("La cuenta destino " + cuentaDestinoId + " se encuentra inactiva o bloqueada");
        }

        // Paso 2: Acreditación local
        destino.setSaldoContable(destino.getSaldoContable() + monto);
        bancoRepository.guardarCuenta(destino);

        Transaccion abono = new Transaccion(
                null,
                cuentaDestinoId,
                LocalDate.now(),
                monto,
                "credito",
                "[SAGA " + sagaId.substring(0, 8) + "] " + (comentario != null ? comentario : "Abono transferencia recibida"),
                canal
        );
        bancoRepository.guardarTransaccion(abono);

        // Actualizar saga a COMPLETADA
        sagaRepository.buscarPorSagaId(sagaId).ifPresent(saga -> {
            saga.setEstado(EstadoSaga.COMPLETADA);
            saga.setFechaActualizacion(LocalDateTime.now());
            saga.setMensaje("Transferencia procesada exitosamente en destino.");
            sagaRepository.guardarSaga(saga);
        });

        log.info("[SAGA-PASO-2] Abono exitoso en cuenta destino {}. Nuevo saldo: {}", cuentaDestinoId, destino.getSaldoContable());
    }

    public synchronized void ejecutarCompensacion(String sagaId, Long cuentaOrigenId, Long monto, String motivoFallo) {
        Cuenta origen = bancoRepository.buscarCuentaPorId(cuentaOrigenId).orElse(null);
        if (origen != null) {
            // Rollback compensatorio
            origen.setSaldoContable(origen.getSaldoContable() + monto);
            bancoRepository.guardarCuenta(origen);

            Transaccion reverso = new Transaccion(
                    null,
                    cuentaOrigenId,
                    LocalDate.now(),
                    monto,
                    "credito",
                    "[COMPENSACION SAGA " + sagaId.substring(0, 8) + "] Reverso por error: " + motivoFallo,
                    "SAGA-ORCHESTRATOR"
            );
            bancoRepository.guardarTransaccion(reverso);

            log.warn("[SAGA-COMPENSACION] Fondos revertidos a cuenta {}. Saldo restituido: {}", cuentaOrigenId, origen.getSaldoContable());
        }

        // Actualizar saga a COMPENSADA
        sagaRepository.buscarPorSagaId(sagaId).ifPresent(saga -> {
            saga.setEstado(EstadoSaga.COMPENSADA);
            saga.setFechaActualizacion(LocalDateTime.now());
            saga.setMensaje("Compensación ejecutada: " + motivoFallo);
            sagaRepository.guardarSaga(saga);
        });
    }
}
