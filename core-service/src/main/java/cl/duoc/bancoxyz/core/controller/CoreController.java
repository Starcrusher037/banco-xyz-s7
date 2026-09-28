package cl.duoc.bancoxyz.core.controller;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.MovimientoAnual;
import cl.duoc.bancoxyz.core.model.Transaccion;
import cl.duoc.bancoxyz.core.service.BancoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core")
@Tag(name = "Core Bancario", description = "Endpoints internos protegidos por Service Token")
public class CoreController {

    private final BancoService bancoService;
    private final cl.duoc.bancoxyz.core.saga.service.SagaTransferenciaOrchestrator sagaOrchestrator;

    public CoreController(BancoService bancoService,
                          cl.duoc.bancoxyz.core.saga.service.SagaTransferenciaOrchestrator sagaOrchestrator) {
        this.bancoService = bancoService;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Operation(summary = "Obtener cuenta por ID")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(bancoService.obtenerCuentaPorId(cuentaId));
    }

    @Operation(summary = "Listar todas las cuentas")
    @GetMapping("/cuentas/todas")
    public ResponseEntity<List<Cuenta>> obtenerTodasLasCuentas() {
        return ResponseEntity.ok(bancoService.obtenerTodasLasCuentas());
    }

    @Operation(summary = "Listar transacciones de una cuenta")
    @GetMapping("/cuentas/{cuentaId}/transacciones")
    public ResponseEntity<List<Transaccion>> obtenerTransacciones(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(bancoService.obtenerTransaccionesPorCuenta(cuentaId));
    }

    @Operation(summary = "Listar movimientos anuales de una cuenta")
    @GetMapping("/cuentas/{cuentaId}/anuales")
    public ResponseEntity<List<MovimientoAnual>> obtenerMovimientosAnuales(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(bancoService.obtenerMovimientosAnualesPorCuenta(cuentaId));
    }

    @Operation(summary = "Ejecutar retiro")
    @PostMapping("/operaciones/retiro")
    public ResponseEntity<Cuenta> ejecutarRetiro(@RequestBody Map<String, Object> req) {
        Long cuentaId = Long.parseLong(req.get("cuentaId").toString());
        Long monto = Long.parseLong(req.get("monto").toString());
        String canal = req.getOrDefault("canal", "ATM").toString();
        return ResponseEntity.ok(bancoService.ejecutarRetiro(cuentaId, monto, canal));
    }

    @Operation(summary = "Ejecutar transferencia mediante patrón SAGA y mensajería JMS")
    @PostMapping("/operaciones/transferencia")
    public ResponseEntity<Map<String, Object>> ejecutarTransferencia(@RequestBody Map<String, Object> req) {
        Long origen = Long.parseLong(req.get("cuentaOrigenId").toString());
        Long destino = Long.parseLong(req.get("cuentaDestinoId").toString());
        Long monto = Long.parseLong(req.get("monto").toString());
        String comentario = req.getOrDefault("comentario", "").toString();
        String canal = req.getOrDefault("canal", "MOVIL").toString();

        cl.duoc.bancoxyz.core.saga.model.RegistroSaga saga = sagaOrchestrator.iniciarTransferenciaSaga(
                origen, destino, monto, comentario, canal
        );

        return ResponseEntity.ok(Map.of(
                "mensaje", "Transferencia iniciada exitosamente bajo arquitectura SAGA asíncrona JMS",
                "sagaId", saga.getSagaId(),
                "estado", saga.getEstado().name(),
                "cuentaOrigenId", saga.getCuentaOrigenId(),
                "cuentaDestinoId", saga.getCuentaDestinoId(),
                "monto", saga.getMonto()
        ));
    }

    @Operation(summary = "Consultar estado de una transacción SAGA")
    @GetMapping("/saga/transacciones/{sagaId}")
    public ResponseEntity<cl.duoc.bancoxyz.core.saga.model.RegistroSaga> obtenerEstadoSaga(@PathVariable String sagaId) {
        return ResponseEntity.ok(sagaOrchestrator.obtenerEstadoSaga(sagaId));
    }

    @Operation(summary = "Listar todas las transacciones SAGA")
    @GetMapping("/saga/transacciones")
    public ResponseEntity<List<cl.duoc.bancoxyz.core.saga.model.RegistroSaga>> listarTransaccionesSaga() {
        return ResponseEntity.ok(sagaOrchestrator.obtenerTodasLasSagas());
    }

    @Operation(summary = "Listar eventos auditados transmitidos por JMS")
    @GetMapping("/saga/auditoria")
    public ResponseEntity<List<cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto>> listarEventosAuditoria() {
        return ResponseEntity.ok(sagaOrchestrator.obtenerEventosAuditoria());
    }
}
