package cl.duoc.bancoxyz.core.controller;

import cl.duoc.bancoxyz.core.dto.SolicitudOperacionCoreDto;
import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.MovimientoAnual;
import cl.duoc.bancoxyz.core.model.Transaccion;
import cl.duoc.bancoxyz.core.service.BancoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core")
@Tag(name = "Core Bancario Legacy API", description = "Endpoints centrales protegidos para acceso exclusivo de los BFF")
public class CoreController {

    private final BancoService bancoService;

    @Value("${bank.core.internal-api-key:SecretCoreKeyBancoXYZ2026}")
    private String expectedApiKey;

    public CoreController(BancoService bancoService) {
        this.bancoService = bancoService;
    }

    private void validarApiKeyInterna(String apiKey) {
        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            throw new RuntimeException("Acceso no autorizado al Core Bancario. API-Key inválida o ausente.");
        }
    }

    @Operation(summary = "Obtener todas las cuentas")
    @GetMapping("/cuentas")
    public ResponseEntity<List<Cuenta>> obtenerTodasLasCuentas(@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        return ResponseEntity.ok(bancoService.obtenerTodasLasCuentas());
    }

    @Operation(summary = "Obtener cuenta por ID")
    @GetMapping("/cuentas/{id}")
    public ResponseEntity<Cuenta> obtenerCuentaPorId(@PathVariable Long id, @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        return bancoService.obtenerCuentaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener transacciones por cuenta")
    @GetMapping("/cuentas/{id}/transacciones")
    public ResponseEntity<List<Transaccion>> obtenerTransacciones(@PathVariable Long id, @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        return ResponseEntity.ok(bancoService.obtenerTransaccionesPorCuenta(id));
    }

    @Operation(summary = "Obtener movimientos anuales históricos")
    @GetMapping("/cuentas/{id}/movimientos-anuales")
    public ResponseEntity<List<MovimientoAnual>> obtenerMovimientosAnuales(@PathVariable Long id, @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        return ResponseEntity.ok(bancoService.obtenerMovimientosAnuales(id));
    }

    @Operation(summary = "Procesar retiro")
    @PostMapping("/operaciones/retiro")
    public ResponseEntity<?> procesarRetiro(@RequestBody SolicitudOperacionCoreDto solicitud, @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        try {
            Transaccion tx = bancoService.procesarRetiro(solicitud.getCuentaId(), solicitud.getMonto(), solicitud.getCanal(), solicitud.getDetalle());
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Procesar depósito")
    @PostMapping("/operaciones/deposito")
    public ResponseEntity<?> procesarDeposito(@RequestBody SolicitudOperacionCoreDto solicitud, @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        try {
            Transaccion tx = bancoService.procesarDeposito(solicitud.getCuentaId(), solicitud.getMonto(), solicitud.getCanal());
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Procesar transferencia")
    @PostMapping("/operaciones/transferencia")
    public ResponseEntity<?> procesarTransferencia(@RequestBody SolicitudOperacionCoreDto solicitud, @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        validarApiKeyInterna(apiKey);
        try {
            Transaccion tx = bancoService.procesarTransferencia(solicitud.getCuentaId(), solicitud.getCuentaDestinoId(), solicitud.getMonto(), solicitud.getDetalle());
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleUnauthorized(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}
