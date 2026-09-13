package cl.duoc.bancoxyz.bff.web.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

@Component
public class CoreClient {

    private final RestClient restClient;

    public CoreClient(@Value("${bank.core.url:https://localhost:8080/api/core}") String coreUrl,
                      @Value("${bank.core.internal-api-key:SecretCoreKeyBancoXYZ2026}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(coreUrl)
                .requestFactory(createSslRequestFactory())
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    private static ClientHttpRequestFactory createSslRequestFactory() {
        try {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());

            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("");

            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .build();

            return new JdkClientHttpRequestFactory(httpClient);
        } catch (Exception e) {
            return new JdkClientHttpRequestFactory();
        }
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
