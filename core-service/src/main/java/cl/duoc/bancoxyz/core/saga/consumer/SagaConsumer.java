package cl.duoc.bancoxyz.core.saga.consumer;

import cl.duoc.bancoxyz.core.saga.config.JmsConfig;
import cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto;
import cl.duoc.bancoxyz.core.saga.event.SagaCompensacionEvent;
import cl.duoc.bancoxyz.core.saga.event.SagaTransferenciaEvent;
import cl.duoc.bancoxyz.core.saga.producer.SagaProducer;
import cl.duoc.bancoxyz.core.saga.repository.SagaRepository;
import cl.duoc.bancoxyz.core.saga.service.SagaStepService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaConsumer.class);

    private final SagaStepService sagaStepService;
    private final SagaProducer sagaProducer;
    private final SagaRepository sagaRepository;

    public SagaConsumer(SagaStepService sagaStepService,
                        SagaProducer sagaProducer,
                        SagaRepository sagaRepository) {
        this.sagaStepService = sagaStepService;
        this.sagaProducer = sagaProducer;
        this.sagaRepository = sagaRepository;
    }

    @JmsListener(destination = JmsConfig.COLA_TRANSFERENCIAS_SAGA)
    @CircuitBreaker(name = "sagaConsumerCB", fallbackMethod = "procesarTransferenciaFallback")
    @Retry(name = "sagaConsumerCB")
    public void procesarTransferencia(SagaTransferenciaEvent event) {
        log.info("[SAGA-CONSUMER] Procesando abono destino para sagaId: {} -> Cuenta Destino: {}",
                event.getSagaId(), event.getCuentaDestinoId());

        try {
            sagaStepService.ejecutarAbonoDestino(
                    event.getSagaId(),
                    event.getCuentaDestinoId(),
                    event.getMonto(),
                    event.getCanal(),
                    event.getComentario()
            );

            sagaProducer.publicarAuditoria(
                    event.getSagaId(),
                    "TRANSFERENCIA_EXITOSA",
                    "Abono realizado con éxito a cuenta destino " + event.getCuentaDestinoId()
            );
        } catch (Exception e) {
            log.error("[SAGA-ERROR] Falló el abono en destino para saga {}: {}. Iniciando compensación...",
                    event.getSagaId(), e.getMessage());

            // Detonar compensación en cola JMS
            SagaCompensacionEvent compensacion = SagaCompensacionEvent.builder()
                    .sagaId(event.getSagaId())
                    .cuentaOrigenId(event.getCuentaOrigenId())
                    .monto(event.getMonto())
                    .canal(event.getCanal())
                    .motivoFallo(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            sagaProducer.enviarCompensacion(compensacion);
        }
    }

    public void procesarTransferenciaFallback(SagaTransferenciaEvent event, Throwable t) {
        log.error("[FALLBACK-SAGA-CONSUMER] Circuito abierto o reintentos agotados para saga {}. Motivo: {}",
                event.getSagaId(), t.getMessage());

        SagaCompensacionEvent compensacion = SagaCompensacionEvent.builder()
                .sagaId(event.getSagaId())
                .cuentaOrigenId(event.getCuentaOrigenId())
                .monto(event.getMonto())
                .canal(event.getCanal())
                .motivoFallo("Fallo crítico en consumidor JMS: " + t.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        sagaProducer.enviarCompensacion(compensacion);
    }

    @JmsListener(destination = JmsConfig.COLA_COMPENSACIONES_SAGA)
    public void procesarCompensacion(SagaCompensacionEvent event) {
        log.warn("[SAGA-CONSUMER] Ejecutando transacción compensatoria para sagaId: {}. Motivo: {}",
                event.getSagaId(), event.getMotivoFallo());

        sagaStepService.ejecutarCompensacion(
                event.getSagaId(),
                event.getCuentaOrigenId(),
                event.getMonto(),
                event.getMotivoFallo()
        );

        sagaProducer.publicarAuditoria(
                event.getSagaId(),
                "COMPENSACION_FINALIZADA",
                "Reverso completado exitosamente para cuenta origen " + event.getCuentaOrigenId()
        );
    }

    @JmsListener(destination = JmsConfig.COLA_AUDITORIA_EVENTOS)
    public void procesarAuditoria(AuditoriaEventoDto evento) {
        log.debug("[AUDITORIA-JMS] Evento recibido: [{}] {} - {}",
                evento.getTipoEvento(), evento.getSagaId(), evento.getDetalle());
        sagaRepository.registrarAuditoria(evento);
    }
}
