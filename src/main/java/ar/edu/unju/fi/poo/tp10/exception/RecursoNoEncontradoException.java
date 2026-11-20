package ar.edu.unju.fi.poo.tp10.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) { super(mensaje); }
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " no encontrado/a con identificador: " + id);
    }
}
