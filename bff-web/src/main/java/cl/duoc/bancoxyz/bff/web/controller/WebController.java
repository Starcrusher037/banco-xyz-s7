package cl.duoc.bancoxyz.bff.web.controller;

import cl.duoc.bancoxyz.bff.web.dto.DashboardWebDto;
import cl.duoc.bancoxyz.bff.web.dto.DetalleCuentaWebDto;
import cl.duoc.bancoxyz.bff.web.dto.TransaccionWebDto;
import cl.duoc.bancoxyz.bff.web.security.JwtUtil;
import cl.duoc.bancoxyz.bff.web.service.WebBffService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/web")
@Tag(name = "BFF Web", description = "Endpoints para portal web (puerto 8082 HTTPS, datos completos con JWT y roles)")
public class WebController {

    private final WebBffService webBffService;
    private final JwtUtil jwtUtil;

    public WebController(WebBffService webBffService, JwtUtil jwtUtil) {
        this.webBffService = webBffService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Login Web para obtener Token JWT")
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credenciales) {
        String username = credenciales.getOrDefault("username", "ejecutivo_web");
        String rol = credenciales.getOrDefault("rol", "ADMIN");
        String token = jwtUtil.generarToken(username, "WEB", rol);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "tipo", "Bearer",
                "canal", "WEB",
                "rol", rol,
                "mensaje", "Autenticación exitosa en Portal Web"
        ));
    }

    private Claims validarTokenWeb(String authHeader, String rolRequerido) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT ausente o formato incorrecto. Enviar cabecera 'Authorization: Bearer <token>'");
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
        if (!"WEB".equalsIgnoreCase(canal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: El token no pertenece al canal WEB (Canal detectado: " + canal + ")");
        }

        // Autorización por Rol (RBAC - Role Based Access Control)
        if (rolRequerido != null) {
            String rol = claims.get("rol", String.class);
            if (rol == null || !rolRequerido.equalsIgnoreCase(rol)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: Se requiere rol '" + rolRequerido + "' para realizar esta operación (Rol detectado: '" + rol + "')");
            }
        }

        return claims;
    }

    @Operation(summary = "Obtener detalle completo de cuenta para Web")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<DetalleCuentaWebDto> obtenerDetalleCuenta(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenWeb(authHeader, null);
        return ResponseEntity.ok(webBffService.obtenerDetalleWeb(cuentaId));
    }

    @Operation(summary = "Listar todas las transacciones históricas")
    @GetMapping("/cuentas/{cuentaId}/transacciones")
    public ResponseEntity<List<TransaccionWebDto>> listarTransacciones(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenWeb(authHeader, null);
        return ResponseEntity.ok(webBffService.listarTodasTransaccionesWeb(cuentaId));
    }

    @Operation(summary = "Dashboard global consolidado (Requiere Rol ADMIN)")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardWebDto> obtenerDashboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenWeb(authHeader, "ADMIN");
        return ResponseEntity.ok(webBffService.obtenerDashboardWeb());
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
