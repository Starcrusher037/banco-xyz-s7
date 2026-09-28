package cl.duoc.bancoxyz.core.saga.service;

import cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto;
import cl.duoc.bancoxyz.core.saga.event.SagaTransferenciaEvent;
import cl.duoc.bancoxyz.core.saga.model.EstadoSaga;
import cl.duoc.bancoxyz.core.saga.model.RegistroSaga;
import cl.duoc.bancoxyz.core.saga.producer.SagaProducer;
import cl.duoc.bancoxyz.core.saga.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SagaTransferenciaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaTransferenciaOrchestrator.class);

    private final SagaStepService sagaStepService;
    private final SagaProducer sagaProducer;
    private final SagaRepository sagaRepository;

    public SagaTransferenciaOrchestrator(SagaStepService sagaStepService,
                                        SagaProducer sagaProducer,
                                        SagaRepository sagaRepository) {
        this.sagaStepService = sagaStepService;
        this.sagaProducer = sagaProducer;
        this.sagaRepository = sagaRepository;
    }

    public RegistroSaga iniciarTransferenciaSaga(Long cuentaOrigenId,
                                                Long cuentaDestinoId,
                                                Long monto,
                                                String comentario,
                                                String canal) {
        String sagaId = UUID.randomUUID().toString();
        log.info("[SAGA-ORQUESTADOR] Iniciando nueva transacción SAGA [{}] Origen: {} -> Destino: {} | Monto: ${}",
                sagaId, cuentaOrigenId, cuentaDestinoId, monto);

        RegistroSaga registro = RegistroSaga.builder()
                .sagaId(sagaId)
                .tipoOperacion("TRANSFERENCIA")
                .cuentaOrigenId(cuentaOrigenId)
                .cuentaDestinoId(cuentaDestinoId)
                .monto(monto)
                .canal(canal != null ? canal : "MOVIL")
                .estado(EstadoSaga.INICIADA)
                .mensaje("Transacción iniciada. Validando cuenta y debitando fondos de origen.")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        sagaRepository.guardarSaga(registro);

        try {
            // Paso 1: Débito atómico en cuenta origen
            sagaStepService.ejecutarDebitoInicial(sagaId, cuentaOrigenId, monto, canal);

            // Publicar evento en broker JMS para procesar paso 2 de forma desacoplada y asíncrona
            SagaTransferenciaEvent event = SagaTransferenciaEvent.builder()
                    .sagaId(sagaId)
                    .cuentaOrigenId(cuentaOrigenId)
                    .cuentaDestinoId(cuentaDestinoId)
                    .monto(monto)
                    .canal(canal)
                    .comentario(comentario)
                    .timestamp(LocalDateTime.now())
                    .build();

            sagaProducer.enviarEventoTransferencia(event);

            registro = sagaRepository.buscarPorSagaId(sagaId).orElse(registro);
            return registro;
        } catch (Exception e) {
            log.error("[SAGA-ORQUESTADOR] Error al iniciar Paso 1 de la Saga [{}]: {}", sagaId, e.getMessage());
            registro.setEstado(EstadoSaga.FALLIDA);
            registro.setMensaje("Fallo en paso inicial: " + e.getMessage());
            registro.setFechaActualizacion(LocalDateTime.now());
            sagaRepository.guardarSaga(registro);
            throw e;
        }
    }

    public RegistroSaga obtenerEstadoSaga(String sagaId) {
        return sagaRepository.buscarPorSagaId(sagaId)
                .orElseThrow(() -> new NoSuchElementException("No existe transacción SAGA con ID: " + sagaId));
    }

    public List<RegistroSaga> obtenerTodasLasSagas() {
        return sagaRepository.obtenerTodasLasSagas();
    }

    public List<AuditoriaEventoDto> obtenerEventosAuditoria() {
        return sagaRepository.obtenerEventosAuditoria();
    }
}
