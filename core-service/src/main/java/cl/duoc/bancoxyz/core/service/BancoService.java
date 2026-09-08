package cl.duoc.bancoxyz.core.service;

import cl.duoc.bancoxyz.core.model.Cuenta;
import cl.duoc.bancoxyz.core.model.MovimientoAnual;
import cl.duoc.bancoxyz.core.model.Transaccion;
import cl.duoc.bancoxyz.core.repository.BancoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BancoService {

    private final BancoRepository bancoRepository;

    public BancoService(BancoRepository bancoRepository) {
        this.bancoRepository = bancoRepository;
    }

    public List<Cuenta> obtenerTodasLasCuentas() {
        return bancoRepository.obtenerTodasLasCuentas();
    }

    public Optional<Cuenta> obtenerCuentaPorId(Long cuentaId) {
        return bancoRepository.buscarCuentaPorId(cuentaId);
    }

    public List<Transaccion> obtenerTransaccionesPorCuenta(Long cuentaId) {
        return bancoRepository.buscarTransaccionesPorCuenta(cuentaId);
    }

    public List<MovimientoAnual> obtenerMovimientosAnuales(Long cuentaId) {
        return bancoRepository.buscarMovimientosAnualesPorCuenta(cuentaId);
    }

    public synchronized Transaccion procesarRetiro(Long cuentaId, Long monto, String canal, String detalleTerminal) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser mayor a $0.");
        }

        Cuenta cuenta = bancoRepository.buscarCuentaPorId(cuentaId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta número " + cuentaId + " no existe."));

        long saldoDisponibleTotal = cuenta.getSaldo() + (cuenta.getLineaSobregiro() != null ? cuenta.getLineaSobregiro() : 0L);
        if (saldoDisponibleTotal < monto) {
            throw new IllegalStateException("Fondos insuficientes. Saldo actual: $" + cuenta.getSaldo());
        }

        cuenta.setSaldo(cuenta.getSaldo() - monto);

        Transaccion tx = new Transaccion(
                System.currentTimeMillis() % 1000000L,
                cuentaId,
                LocalDate.now().toString(),
                monto,
                "retiro",
                detalleTerminal != null ? detalleTerminal : "Retiro de efectivo en " + canal,
                canal
        );

        bancoRepository.guardarTransaccion(tx);
        return tx;
    }

    public synchronized Transaccion procesarDeposito(Long cuentaId, Long monto, String canal) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto a depositar debe ser mayor a $0.");
        }

        Cuenta cuenta = bancoRepository.buscarCuentaPorId(cuentaId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta número " + cuentaId + " no existe."));

        cuenta.setSaldo(cuenta.getSaldo() + monto);

        Transaccion tx = new Transaccion(
                System.currentTimeMillis() % 1000000L,
                cuentaId,
                LocalDate.now().toString(),
                monto,
                "abono",
                "Depósito de fondos vía " + canal,
                canal
        );

        bancoRepository.guardarTransaccion(tx);
        return tx;
    }

    public synchronized Transaccion procesarTransferencia(Long cuentaOrigenId, Long cuentaDestinoId, Long monto, String descripcion) {
        if (cuentaOrigenId.equals(cuentaDestinoId)) {
            throw new IllegalArgumentException("No se puede transferir a la misma cuenta de origen.");
        }

        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto a transferir debe ser mayor a $0.");
        }

        Cuenta origen = bancoRepository.buscarCuentaPorId(cuentaOrigenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no encontrada: " + cuentaOrigenId));

        Cuenta destino = bancoRepository.buscarCuentaPorId(cuentaDestinoId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no encontrada: " + cuentaDestinoId));

        long saldoDisponible = origen.getSaldo() + (origen.getLineaSobregiro() != null ? origen.getLineaSobregiro() : 0L);
        if (saldoDisponible < monto) {
            throw new IllegalStateException("Saldo insuficiente para transferir $" + monto);
        }

        origen.setSaldo(origen.getSaldo() - monto);
        destino.setSaldo(destino.getSaldo() + monto);

        Transaccion txOrigen = new Transaccion(
                System.currentTimeMillis() % 1000000L,
                cuentaOrigenId,
                LocalDate.now().toString(),
                monto,
                "transferencia_saliente",
                descripcion != null ? descripcion : "Transferencia a cuenta " + cuentaDestinoId,
                "MOVIL"
        );
        bancoRepository.guardarTransaccion(txOrigen);

        Transaccion txDestino = new Transaccion(
                (System.currentTimeMillis() + 1) % 1000000L,
                cuentaDestinoId,
                LocalDate.now().toString(),
                monto,
                "transferencia_entrante",
                "Transferencia recibida desde cuenta " + cuentaOrigenId,
                "MOVIL"
        );
        bancoRepository.guardarTransaccion(txDestino);

        return txOrigen;
    }
}
