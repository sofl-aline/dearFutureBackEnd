package br.edu.uniesp.dearfuture.shared.exception;

import java.util.Map;
public class ApiException extends RuntimeException {
    public enum Code { VALIDATION_ERROR, CAPSULE_NOT_FOUND, CAPSULE_SEALED,
        EMAIL_ALREADY_REGISTERED, INVALID_CREDENTIALS, UNAUTHENTICATED, ACCESS_DENIED }
    private final Code code;
    private final Map<String, String> errors;
    public ApiException(Code code, String message) { this(code, message, Map.of()); }
    public ApiException(Code code, String message, Map<String, String> errors) {
        super(message);
        this.code = code;
        this.errors = Map.copyOf(errors);
    }
    public Code getCode() { return code; }
    public Map<String, String> getErrors() { return errors; }
}
