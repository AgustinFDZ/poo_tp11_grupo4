package ar.edu.unju.fi.poo.tp10.exception;

public class ClienteInhabilitadoException extends RuntimeException {
    public ClienteInhabilitadoException(String nombre) {
        super("El cliente '" + nombre + "' está inhabilitado y no puede registrar compras");
    }
}
