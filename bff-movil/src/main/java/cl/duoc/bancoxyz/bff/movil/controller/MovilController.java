package cl.duoc.bancoxyz.bff.movil.controller;

import cl.duoc.bancoxyz.bff.movil.dto.ResumenCuentaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.SolicitudTransferenciaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import cl.duoc.bancoxyz.bff.movil.security.JwtUtil;
import cl.duoc.bancoxyz.bff.movil.service.MovilBffService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/movil")
@Tag(name = "BFF Móvil", description = "Endpoints para app móvil (puerto 8081 HTTPS, payloads ligeros con JWT)")
public class MovilController {

    private final MovilBffService movilBffService;
    private final JwtUtil jwtUtil;

    public MovilController(MovilBffService movilBffService, JwtUtil jwtUtil) {
        this.movilBffService = movilBffService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Login Móvil para obtener Token JWT")
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credenciales) {
        String username = credenciales.getOrDefault("username", "usuario_movil");
        Long cuentaId = Long.parseLong(credenciales.getOrDefault("cuentaId", "101"));
        String token = jwtUtil.generarToken(username, "MOVIL", cuentaId);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "tipo", "Bearer",
                "canal", "MOVIL",
                "cuentaId", cuentaId,
                "mensaje", "Autenticación exitosa en Canal Móvil"
        ));
    }

    private Claims validarTokenMovil(String authHeader, Long cuentaIdEsperada) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT ausente o formato incorrecto. Enviar en cabecera 'Authorization: Bearer <token>'");
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
        if (!"MOVIL".equalsIgnoreCase(canal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: El token no pertenece al canal MOVIL (Canal detectado: " + canal + ")");
        }

        // Autorización por Cuenta (Principio de menor privilegio)
        if (cuentaIdEsperada != null) {
            Number tokenCuentaIdNum = claims.get("cuentaId", Number.class);
            Long tokenCuentaId = tokenCuentaIdNum != null ? tokenCuentaIdNum.longValue() : null;
            if (tokenCuentaId != null && !tokenCuentaId.equals(cuentaIdEsperada)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: El token pertenece a la cuenta " + tokenCuentaId + " y no está autorizado para acceder a la cuenta " + cuentaIdEsperada);
            }
        }

        return claims;
    }

    @Operation(summary = "Obtener resumen de cuenta para móvil")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<ResumenCuentaMovilDto> obtenerResumenCuenta(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenMovil(authHeader, cuentaId);
        return ResponseEntity.ok(movilBffService.obtenerResumenMovil(cuentaId));
    }

    @Operation(summary = "Consulta rápida de saldo móvil")
    @GetMapping("/cuentas/{cuentaId}/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldoRapido(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenMovil(authHeader, cuentaId);
        Long saldo = movilBffService.consultarSaldoMovil(cuentaId);
        return ResponseEntity.ok(Map.of("cuentaId", cuentaId, "saldoDisponible", saldo));
    }

    @Operation(summary = "Transferencia rápida móvil")
    @PostMapping("/cuentas/{cuentaId}/transferencia")
    public ResponseEntity<TransaccionMovilDto> transferir(
            @PathVariable Long cuentaId,
            @RequestBody SolicitudTransferenciaMovilDto solicitud,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenMovil(authHeader, cuentaId);
        TransaccionMovilDto respuesta = movilBffService.transferirMovil(cuentaId, solicitud);
        return ResponseEntity.ok(respuesta);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "error", ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                "status", ex.getStatusCode().value()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", ex.getMessage(),
                "status", HttpStatus.BAD_REQUEST.value()
        ));
    }
}
