package ar.edu.unju.fi.poo.tp10.exception;

/** Conflicto de estado: venta ya anulada, cuota ya pagada, etc. */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) { super(mensaje); }
}
