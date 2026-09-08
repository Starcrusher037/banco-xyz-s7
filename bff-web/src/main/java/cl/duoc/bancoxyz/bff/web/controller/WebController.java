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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/web")
@Tag(name = "BFF Web", description = "Endpoints para portal web (puerto 8082, datos completos con JWT)")
public class WebController {

    private final WebBffService webBffService;
    private final JwtUtil jwtUtil;

    public WebController(WebBffService webBffService, JwtUtil jwtUtil) {
        this.webBffService = webBffService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Login Web para obtener Token JWT")
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credenciales) {
        String username = credenciales.getOrDefault("username", "ejecutivo_web");
        String rol = credenciales.getOrDefault("rol", "ADMIN");
        String token = jwtUtil.generarToken(username, "WEB", rol);
        return ResponseEntity.ok(Map.of("token", token, "tipo", "Bearer", "canal", "WEB"));
    }

    private void validarTokenWeb(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT ausente o formato incorrecto. Enviar cabecera 'Authorization: Bearer <token>'");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.validarToken(token);
        if (!"WEB".equalsIgnoreCase(claims.get("canal", String.class))) {
            throw new RuntimeException("Canal no autorizado para este token JWT.");
        }
    }

    @Operation(summary = "Obtener detalle completo de cuenta para Web")
    @GetMapping("/cuentas/{cuentaId}")
    public ResponseEntity<DetalleCuentaWebDto> obtenerDetalleCuenta(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenWeb(authHeader);
        return ResponseEntity.ok(webBffService.obtenerDetalleWeb(cuentaId));
    }

    @Operation(summary = "Listar todas las transacciones históricas")
    @GetMapping("/cuentas/{cuentaId}/transacciones")
    public ResponseEntity<List<TransaccionWebDto>> listarTransacciones(
            @PathVariable Long cuentaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenWeb(authHeader);
        return ResponseEntity.ok(webBffService.listarTodasTransaccionesWeb(cuentaId));
    }

    @Operation(summary = "Dashboard global consolidado")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardWebDto> obtenerDashboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validarTokenWeb(authHeader);
        return ResponseEntity.ok(webBffService.obtenerDashboardWeb());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleAuthError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }
}
