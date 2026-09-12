package cl.duoc.bancoxyz.bff.cajero.controller;

import cl.duoc.bancoxyz.bff.cajero.dto.ConsultaSaldoCajeroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.RespuestaRetiroDto;
import cl.duoc.bancoxyz.bff.cajero.dto.SolicitudRetiroCajeroDto;
import cl.duoc.bancoxyz.bff.cajero.security.JwtUtil;
import cl.duoc.bancoxyz.bff.cajero.service.CajeroBffService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cajero")
@Tag(name = "BFF Cajero Automático", description = "Endpoints para cajeros ATM (puerto 8083 HTTPS, giros $5.000 con JWT)")
public class CajeroController {

    private final CajeroBffService cajeroBffService;
    private final JwtUtil jwtUtil;

    public CajeroController(CajeroBffService cajeroBffService, JwtUtil jwtUtil) {
        this.cajeroBffService = cajeroBffService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Autenticar Terminal ATM para obtener Token JWT")
    @PostMapping("/auth/token")
    public ResponseEntity<Map<String, Object>> loginTerminal(@RequestBody Map<String, String> body) {
        String terminalId = body.getOrDefault("terminalId", "ATM-SCL-01");
        String token = jwtUtil.generarTokenTerminal(terminalId, "CAJERO_ATM");
        return ResponseEntity.ok(Map.of(
                "token", token,
                "tipo", "Bearer",
                "canal", "CAJERO_ATM",
                "terminalId", terminalId,
                "mensaje", "Terminal ATM autenticado correctamente"
        ));
    }

    private Claims validarTokenCajero(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Terminal ATM ausente o formato incorrecto. Enviar 'Authorization: Bearer <token>'");
        }
        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.validarToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT inválido o expirado: " + e.getMessage());
        }

        // Autorización de Canal
        String canal = claims.get("canal", String.class);
        if (!"CAJERO_ATM".equalsIgnoreCase(canal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: El token no pertenece al canal CAJERO_ATM (Canal detectado: " + canal + ")");
        }

        return claims;
    }

    @Operation(summary = "Consulta express de saldo en Cajero Automático")
    @GetMapping("/cuentas/{cuentaId}/saldo")
    public ResponseEntity<ConsultaSaldoCajeroDto> consultarSaldo(
            @PathVariable Long cuentaId,
            @RequestParam(required = false, defaultValue = "ATM-SCL-01") String terminalId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenCajero(authHeader);
        return ResponseEntity.ok(cajeroBffService.consultarSaldoCajero(cuentaId, terminalId));
    }

    @Operation(summary = "Retiro de dinero en efectivo")
    @PostMapping("/cuentas/{cuentaId}/retiro")
    public ResponseEntity<RespuestaRetiroDto> procesarRetiro(
            @PathVariable Long cuentaId,
            @RequestBody SolicitudRetiroCajeroDto solicitud,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenCajero(authHeader);
        return ResponseEntity.ok(cajeroBffService.procesarRetiroCajero(cuentaId, solicitud));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "error", ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                "status", ex.getStatusCode().value()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleCajeroError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage(),
                "status", HttpStatus.BAD_REQUEST.value()
        ));
    }
}
