package br.edu.uniesp.dearfuture.config.exception;

import br.edu.uniesp.dearfuture.config.ProblemResponses;
import br.edu.uniesp.dearfuture.shared.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponses problems;
    public GlobalExceptionHandler(ProblemResponses problems) { this.problems = problems; }
    @ExceptionHandler(ApiException.class)
    public ProblemDetail business(ApiException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case CAPSULE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CAPSULE_SEALED, EMAIL_ALREADY_REGISTERED -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS, UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
        return problems.create(status, exception.getCode().name(), exception.getMessage(),
                request.getRequestURI(), exception.getErrors());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return problems.create(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revise os campos informados.", request.getRequestURI(), errors);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail malformed(Exception exception, HttpServletRequest request) {
        return problems.create(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Requisição inválida.", request.getRequestURI(), Map.of());
    }
    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public ProblemDetail framework(org.springframework.web.ErrorResponseException exception, HttpServletRequest request) {
        return problems.create(HttpStatus.valueOf(exception.getStatusCode().value()), "HTTP_ERROR",
                "Não foi possível atender esta requisição.", request.getRequestURI(), Map.of());
    }
    @ExceptionHandler({org.springframework.web.HttpRequestMethodNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public ProblemDetail protocol(Exception exception, HttpServletRequest request) {
        var error = (org.springframework.web.ErrorResponse) exception;
        return problems.create(HttpStatus.valueOf(error.getStatusCode().value()), "HTTP_ERROR",
                "Método, conteúdo ou recurso não disponível.", request.getRequestURI(), Map.of());
    }
    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        // Não registrar a exceção inteira: mensagens de SQL/conversão podem conter cartas ou senhas.
        String event = java.util.UUID.randomUUID().toString();
        LOG.error("Falha interna: evento={}, tipo={}", event, exception.getClass().getName());
        return problems.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Falha interna. Referência: " + event, request.getRequestURI(), Map.of());
    }
}