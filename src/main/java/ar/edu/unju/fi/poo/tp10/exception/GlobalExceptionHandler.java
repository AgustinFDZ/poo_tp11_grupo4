package ar.edu.unju.fi.poo.tp10.exception;

import ar.edu.unju.fi.poo.tp10.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 404 ----
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    // ---- 409 ----
    @ExceptionHandler({DatoDuplicadoException.class, StockInsuficienteException.class, OperacionNoPermitidaException.class})
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, "La operación viola una restricción de integridad de datos", req, null);
    }

    // ---- 422 ----
    @ExceptionHandler({ClienteInhabilitadoException.class, ImporteInvalidoException.class})
    public ResponseEntity<ErrorResponse> noProcesable(RuntimeException ex, HttpServletRequest req) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, null);
    }

    // ---- 400 ----
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> solicitudInvalida(SolicitudInvalidaException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", req, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido o tiene un formato incorrecto", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoIncorrecto(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue(), req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", req, null);
    }

    // ---- 500 ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generico(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno en el servidor", req, null);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje,
                                                    HttpServletRequest req, List<String> detalles) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(body);
    }
}
