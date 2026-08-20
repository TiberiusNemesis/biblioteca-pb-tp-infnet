package br.edu.infnet.loans.config;

import br.edu.infnet.loans.exception.ActiveLoanExistsException;
import br.edu.infnet.loans.exception.CatalogBookNotFoundException;
import br.edu.infnet.loans.exception.CatalogUnavailableException;
import br.edu.infnet.loans.exception.LoanAlreadyReturnedException;
import br.edu.infnet.loans.exception.LoanNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLoanNotFound(LoanNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "loan_not_found", ex.getMessage());
    }

    @ExceptionHandler(CatalogBookNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogBookNotFound(CatalogBookNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "catalog_book_not_found", ex.getMessage());
    }

    @ExceptionHandler(ActiveLoanExistsException.class)
    public ResponseEntity<Map<String, Object>> handleActiveLoan(ActiveLoanExistsException ex) {
        return response(HttpStatus.CONFLICT, "active_loan_exists", ex.getMessage());
    }

    @ExceptionHandler(LoanAlreadyReturnedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyReturned(LoanAlreadyReturnedException ex) {
        return response(HttpStatus.CONFLICT, "loan_already_returned", ex.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogUnavailable(CatalogUnavailableException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "catalog_unavailable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())
                ))
                .toList();
        Map<String, Object> body = error("validation_error", "Request body has invalid fields");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private static ResponseEntity<Map<String, Object>> response(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity.status(status).body(error(code, message));
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}
