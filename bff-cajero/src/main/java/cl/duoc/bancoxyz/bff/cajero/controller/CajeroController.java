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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cajero")
@Tag(name = "BFF Cajero Automático", description = "Endpoints para cajeros ATM (puerto 8083, giros $5.000 con JWT)")
public class CajeroController {

    private final CajeroBffService cajeroBffService;
    private final JwtUtil jwtUtil;

    public CajeroController(CajeroBffService cajeroBffService, JwtUtil jwtUtil) {
        this.cajeroBffService = cajeroBffService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Autenticar Terminal ATM para obtener Token JWT")
    @PostMapping("/auth/token")
    public ResponseEntity<Map<String, String>> loginTerminal(@RequestBody Map<String, String> body) {
        String terminalId = body.getOrDefault("terminalId", "ATM-SCL-01");
        String token = jwtUtil.generarTokenTerminal(terminalId, "CAJERO_ATM");
        return ResponseEntity.ok(Map.of("token", token, "tipo", "Bearer", "canal", "CAJERO_ATM"));
    }

    private void validarTokenCajero(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token de Terminal ATM ausente o formato incorrecto. Enviar 'Authorization: Bearer <token>'");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.validarToken(token);
        if (!"CAJERO_ATM".equalsIgnoreCase(claims.get("canal", String.class))) {
            throw new RuntimeException("Canal no autorizado para este token.");
        }
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleCajeroError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
