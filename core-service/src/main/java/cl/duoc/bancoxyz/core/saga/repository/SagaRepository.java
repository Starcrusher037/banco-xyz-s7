package cl.duoc.bancoxyz.core.saga.repository;

import cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto;
import cl.duoc.bancoxyz.core.saga.model.RegistroSaga;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SagaRepository {

    private final Map<String, RegistroSaga> sagas = new ConcurrentHashMap<>();
    private final List<AuditoriaEventoDto> eventosAuditoria = Collections.synchronizedList(new ArrayList<>());

    public void guardarSaga(RegistroSaga saga) {
        sagas.put(saga.getSagaId(), saga);
    }

    public Optional<RegistroSaga> buscarPorSagaId(String sagaId) {
        return Optional.ofNullable(sagas.get(sagaId));
    }

    public List<RegistroSaga> obtenerTodasLasSagas() {
        return new ArrayList<>(sagas.values());
    }

    public void registrarAuditoria(AuditoriaEventoDto evento) {
        eventosAuditoria.add(evento);
    }

    public List<AuditoriaEventoDto> obtenerEventosAuditoria() {
        return new ArrayList<>(eventosAuditoria);
    }
}
