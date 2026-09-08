package cl.duoc.bancoxyz.bff.movil.controller;

import cl.duoc.bancoxyz.bff.movil.dto.ResumenCuentaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.SolicitudTransferenciaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import cl.duoc.bancoxyz.bff.movil.security.JwtUtil;
import cl.duoc.bancoxyz.bff.movil.service.MovilBffService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/movil")
@Tag(name = "BFF Móvil", description = "Endpoints para app móvil (puerto 8081, payloads ligeros con JWT)")
public class MovilController {

    private final MovilBffService movilBffService;
    private final JwtUtil jwtUtil;

    public MovilController(MovilBffService movilBffService, JwtUtil jwtUtil) {
        this.movilBffService = movilBffService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Login Móvil para obtener Token JWT")
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credenciales) {
        String username = credenciales.getOrDefault("username", "usuario_movil");
        Long cuentaId = Long.parseLong(credenciales.getOrDefault("cuentaId", "101"));
        String token = jwtUtil.generarToken(username, "MOVIL", cuentaId);
        return ResponseEntity.ok(Map.of("token", token, "tipo", "Bearer", "canal", "MOVIL"));
    }

    private void validarTokenMovil(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT ausente o formato incorrecto. Enviar en cabecera 'Authorization: Bearer <token>'");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.validarToken(token);
        if (!"MOVIL".equalsIgnoreCase(claims.get("canal", String.class))) {
            throw new RuntimeException("Canal no autorizado para este token JWT.");
        }
    }

    @Operation(summary = "Obtener resumen de cuenta para móvil")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<ResumenCuentaMovilDto> obtenerResumenCuenta(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenMovil(authHeader);
        return ResponseEntity.ok(movilBffService.obtenerResumenMovil(cuentaId));
    }

    @Operation(summary = "Consulta rápida de saldo móvil")
    @GetMapping("/cuentas/{cuentaId}/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldoRapido(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenMovil(authHeader);
        Long saldo = movilBffService.consultarSaldoMovil(cuentaId);
        return ResponseEntity.ok(Map.of("cuentaId", cuentaId, "saldoDisponible", saldo));
    }

    @Operation(summary = "Transferencia rápida móvil")
    @PostMapping("/cuentas/{cuentaId}/transferencia")
    public ResponseEntity<TransaccionMovilDto> transferir(
            @PathVariable Long cuentaId,
            @RequestBody SolicitudTransferenciaMovilDto solicitud,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenMovil(authHeader);
        TransaccionMovilDto respuesta = movilBffService.transferirMovil(cuentaId, solicitud);
        return ResponseEntity.ok(respuesta);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleAuthError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }
}
