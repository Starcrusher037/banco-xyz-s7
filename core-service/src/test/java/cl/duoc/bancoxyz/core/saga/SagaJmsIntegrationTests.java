package cl.duoc.bancoxyz.core.saga;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.repository.BancoRepository;
import cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto;
import cl.duoc.bancoxyz.core.saga.model.EstadoSaga;
import cl.duoc.bancoxyz.core.saga.model.RegistroSaga;
import cl.duoc.bancoxyz.core.saga.service.SagaTransferenciaOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class SagaJmsIntegrationTests {

    @Autowired
    private SagaTransferenciaOrchestrator orchestrator;

    @Autowired
    private BancoRepository bancoRepository;

    @Test
    @DisplayName("SAGA Exitosa: Debita origen, emite evento JMS, acredita destino y completa estado")
    void transferenciaExitosaFlujoCompletoSagaJms() {
        Cuenta origen = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
        Cuenta destino = bancoRepository.buscarCuentaPorId(107L).orElseThrow();

        long saldoInicialOrigen = 100_000L;
        long saldoInicialDestino = 50_000L;
        long montoTransferir = 15_000L;

        origen.setSaldoContable(saldoInicialOrigen);
        destino.setSaldoContable(saldoInicialDestino);
        bancoRepository.guardarCuenta(origen);
        bancoRepository.guardarCuenta(destino);

        RegistroSaga iniciada = orchestrator.iniciarTransferenciaSaga(
                106L, 107L, montoTransferir, "Abono SAGA test exitoso", "MOVIL"
        );

        assertThat(iniciada).isNotNull();
        assertThat(iniciada.getSagaId()).isNotBlank();
        assertThat(iniciada.getEstado()).isIn(EstadoSaga.DEBITO_COMPLETADO, EstadoSaga.COMPLETADA);

        // Esperar procesamiento asíncrono en broker JMS embebido ActiveMQ Artemis
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            RegistroSaga actual = orchestrator.obtenerEstadoSaga(iniciada.getSagaId());
            assertThat(actual.getEstado()).isEqualTo(EstadoSaga.COMPLETADA);

            Cuenta cOrigen = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
            Cuenta cDestino = bancoRepository.buscarCuentaPorId(107L).orElseThrow();

            assertThat(cOrigen.getSaldoContable()).isEqualTo(saldoInicialOrigen - montoTransferir);
            assertThat(cDestino.getSaldoContable()).isEqualTo(saldoInicialDestino + montoTransferir);
        });

        // Verificar registro de auditoría JMS
        List<AuditoriaEventoDto> auditoria = orchestrator.obtenerEventosAuditoria();
        assertThat(auditoria).anyMatch(a -> a.getSagaId().equals(iniciada.getSagaId())
                && "TRANSFERENCIA_INICIADA".equals(a.getTipoEvento()));
        assertThat(auditoria).anyMatch(a -> a.getSagaId().equals(iniciada.getSagaId())
                && "TRANSFERENCIA_EXITOSA".equals(a.getTipoEvento()));
    }

    @Test
    @DisplayName("SAGA Compensatoria (Rollback): Fallo en cuenta destino activa compensación JMS y restituye saldo")
    void transferenciaFallidaActivaCompensacionRollbackSagaJms() {
        Cuenta origen = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
        long saldoInicialOrigen = 80_000L;
        long montoTransferir = 20_000L;
        Long cuentaInexistente = 999999L;

        origen.setSaldoContable(saldoInicialOrigen);
        bancoRepository.guardarCuenta(origen);

        RegistroSaga saga = orchestrator.iniciarTransferenciaSaga(
                106L, cuentaInexistente, montoTransferir, "Test transferencia a cuenta errónea", "WEB"
        );

        assertThat(saga).isNotNull();

        // En el momento inmediato se debitó el origen (Paso 1)
        Cuenta origenInmediato = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
        assertThat(origenInmediato.getSaldoContable()).isEqualTo(saldoInicialOrigen - montoTransferir);

        // El broker JMS recibe el evento, el consumidor falla en el abono y dispara la compensación automática
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            RegistroSaga actual = orchestrator.obtenerEstadoSaga(saga.getSagaId());
            assertThat(actual.getEstado()).isEqualTo(EstadoSaga.COMPENSADA);

            Cuenta cOrigenRestituida = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
            // El saldo debe ser exactamente el saldo inicial gracias a la transacción compensatoria
            assertThat(cOrigenRestituida.getSaldoContable()).isEqualTo(saldoInicialOrigen);
        });

        // Verificar que los eventos de compensación fueron emitidos y procesados por JMS
        List<AuditoriaEventoDto> auditoria = orchestrator.obtenerEventosAuditoria();
        assertThat(auditoria).anyMatch(a -> a.getSagaId().equals(saga.getSagaId())
                && "COMPENSACION_SOLICITADA".equals(a.getTipoEvento()));
        assertThat(auditoria).anyMatch(a -> a.getSagaId().equals(saga.getSagaId())
                && "COMPENSACION_FINALIZADA".equals(a.getTipoEvento()));
    }

    @Test
    @DisplayName("SAGA Rechazo Inicial: Saldo insuficiente cancela en Paso 1 sin emitir a destino")
    void saldoInsuficienteRechazaInmediatamente() {
        Cuenta origen = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
        origen.setSaldoContable(10_000L);
        origen.setLineaSobregiro(0L);
        bancoRepository.guardarCuenta(origen);

        assertThatThrownBy(() -> orchestrator.iniciarTransferenciaSaga(
                106L, 107L, 500_000L, "Fondo insuficiente", "CAJERO"
        )).isInstanceOf(IllegalArgumentException.class);

        // El saldo debe permanecer intacto
        Cuenta origenSinCambio = bancoRepository.buscarCuentaPorId(106L).orElseThrow();
        assertThat(origenSinCambio.getSaldoContable()).isEqualTo(10_000L);
    }
}
