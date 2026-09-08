package cl.duoc.bancoxyz.bff.web.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    public List<Map<String, Object>> obtenerTodasLasCuentas() {
        return restClient.get()
                .uri("/cuentas")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
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

    public List<Map<String, Object>> obtenerMovimientosAnuales(Long cuentaId) {
        return restClient.get()
                .uri("/cuentas/{id}/movimientos-anuales", cuentaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
