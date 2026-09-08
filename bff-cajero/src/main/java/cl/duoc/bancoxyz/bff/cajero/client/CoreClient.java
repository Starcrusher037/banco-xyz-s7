package cl.duoc.bancoxyz.bff.cajero.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public Map<String, Object> procesarRetiro(Long cuentaId, Long monto, String canal, String detalle) {
        Map<String, Object> body = Map.of(
                "cuentaId", cuentaId,
                "monto", monto,
                "canal", canal,
                "detalle", detalle
        );
        return restClient.post()
                .uri("/operaciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
