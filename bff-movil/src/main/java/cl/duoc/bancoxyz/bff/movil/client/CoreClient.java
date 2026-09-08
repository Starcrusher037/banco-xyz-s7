package cl.duoc.bancoxyz.bff.movil.client;

import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class CoreClient {

    private final RestClient restClient;

    public CoreClient(@Value("${bank.core.url:http://localhost:8080/api/core}") String coreUrl,
                      @Value("${bank.core.internal-api-key:SecretCoreKeyBancoXYZ2026}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(coreUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    public Map<String, Object> obtenerCuentaPorId(Long cuentaId) {
        return restClient.get()
                .uri("/cuentas/{id}", cuentaId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public List<Map<String, Object>> obtenerTransaccionesPorCuenta(Long cuentaId) {
        return restClient.get()
                .uri("/cuentas/{id}/transacciones", cuentaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public Map<String, Object> procesarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Long monto, String detalle) {
        Map<String, Object> body = Map.of(
                "cuentaId", cuentaOrigenId,
                "cuentaDestinoId", cuentaDestinoId,
                "monto", monto,
                "canal", "MOVIL",
                "detalle", detalle != null ? detalle : "Transferencia móvil"
        );
        return restClient.post()
                .uri("/operaciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
