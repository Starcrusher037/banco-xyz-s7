package cl.duoc.bancoxyz.core.saga.producer;

import cl.duoc.bancoxyz.core.saga.config.JmsConfig;
import cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto;
import cl.duoc.bancoxyz.core.saga.event.SagaCompensacionEvent;
import cl.duoc.bancoxyz.core.saga.event.SagaTransferenciaEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SagaProducer {

    private static final Logger log = LoggerFactory.getLogger(SagaProducer.class);
    private final JmsTemplate jmsTemplate;

    public SagaProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @CircuitBreaker(name = "sagaJmsCB", fallbackMethod = "enviarEventoTransferenciaFallback")
    @Retry(name = "sagaJmsCB")
    public void enviarEventoTransferencia(SagaTransferenciaEvent event) {
        log.info("[SAGA-PRODUCER] Publicando evento de transferencia {} a cola {}", event.getSagaId(), JmsConfig.COLA_TRANSFERENCIAS_SAGA);
        jmsTemplate.convertAndSend(JmsConfig.COLA_TRANSFERENCIAS_SAGA, event);
        publicarAuditoria(event.getSagaId(), "TRANSFERENCIA_INICIADA", "Evento de transferencia enviado al broker JMS para saga: " + event.getSagaId());
    }

    public void enviarEventoTransferenciaFallback(SagaTransferenciaEvent event, Throwable t) {
        log.error("[FALLBACK-SAGA-PRODUCER] Error crítico al publicar transferencia {}: {}", event.getSagaId(), t.getMessage());
        throw new IllegalStateException("El sistema de mensajería JMS no se encuentra disponible. Motivo: " + t.getMessage(), t);
    }

    @CircuitBreaker(name = "sagaJmsCB", fallbackMethod = "enviarCompensacionFallback")
    @Retry(name = "sagaJmsCB")
    public void enviarCompensacion(SagaCompensacionEvent event) {
        log.warn("[SAGA-PRODUCER] Publicando compensación {} a cola {}", event.getSagaId(), JmsConfig.COLA_COMPENSACIONES_SAGA);
        jmsTemplate.convertAndSend(JmsConfig.COLA_COMPENSACIONES_SAGA, event);
        publicarAuditoria(event.getSagaId(), "COMPENSACION_SOLICITADA", "Compensación encolada. Motivo: " + event.getMotivoFallo());
    }

    public void enviarCompensacionFallback(SagaCompensacionEvent event, Throwable t) {
        log.error("[FALLBACK-SAGA-PRODUCER] Error crítico al encolar compensación {}: {}", event.getSagaId(), t.getMessage());
    }

    public void publicarAuditoria(String sagaId, String tipoEvento, String detalle) {
        try {
            AuditoriaEventoDto auditoria = AuditoriaEventoDto.builder()
                    .eventoId(UUID.randomUUID().toString())
                    .sagaId(sagaId)
                    .tipoEvento(tipoEvento)
                    .detalle(detalle)
                    .timestamp(LocalDateTime.now())
                    .build();
            jmsTemplate.convertAndSend(JmsConfig.COLA_AUDITORIA_EVENTOS, auditoria);
        } catch (Exception e) {
            log.warn("[SAGA-PRODUCER] No fue posible publicar auditoría JMS: {}", e.getMessage());
        }
    }
}
