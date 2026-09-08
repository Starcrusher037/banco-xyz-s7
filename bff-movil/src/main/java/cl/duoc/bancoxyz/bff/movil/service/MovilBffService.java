package cl.duoc.bancoxyz.bff.movil.service;

import cl.duoc.bancoxyz.bff.movil.client.CoreClient;
import cl.duoc.bancoxyz.bff.movil.dto.ResumenCuentaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.SolicitudTransferenciaMovilDto;
import cl.duoc.bancoxyz.bff.movil.dto.TransaccionMovilDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MovilBffService {

    private final CoreClient coreClient;

    public MovilBffService(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    public ResumenCuentaMovilDto obtenerResumenMovil(Long cuentaId) {
        Map<String, Object> cuenta = coreClient.obtenerCuentaPorId(cuentaId);
        if (cuenta == null) {
            throw new RuntimeException("Cuenta no encontrada: " + cuentaId);
        }

        List<Map<String, Object>> rawTx = coreClient.obtenerTransaccionesPorCuenta(cuentaId);
        List<TransaccionMovilDto> ultimas3 = rawTx.stream()
                .limit(3)
                .map(this::mapToMovilTx)
                .collect(Collectors.toList());

        String titular = (String) cuenta.get("nombreTitular");
        Long saldo = ((Number) cuenta.get("saldo")).longValue();
        String tipo = (String) cuenta.get("tipoCuenta");

        return new ResumenCuentaMovilDto(cuentaId, titular, saldo, tipo, ultimas3, "Saldo actualizado en tiempo real.");
    }

    public Long consultarSaldoMovil(Long cuentaId) {
        Map<String, Object> cuenta = coreClient.obtenerCuentaPorId(cuentaId);
        if (cuenta == null) {
            throw new RuntimeException("Cuenta no encontrada: " + cuentaId);
        }
        return ((Number) cuenta.get("saldo")).longValue();
    }

    public TransaccionMovilDto transferirMovil(Long cuentaOrigenId, SolicitudTransferenciaMovilDto solicitud) {
        Map<String, Object> res = coreClient.procesarTransferencia(
                cuentaOrigenId,
                solicitud.getCuentaDestinoId(),
                solicitud.getMonto(),
                solicitud.getComentario()
        );
        return mapToMovilTx(res);
    }

    private TransaccionMovilDto mapToMovilTx(Map<String, Object> tx) {
        Long id = ((Number) tx.get("id")).longValue();
        String fecha = (String) tx.get("fecha");
        Long monto = ((Number) tx.get("monto")).longValue();
        String tipo = (String) tx.get("tipo");
        String descripcion = (String) tx.get("descripcion");
        return new TransaccionMovilDto(id, fecha, monto, tipo, descripcion);
    }
}
