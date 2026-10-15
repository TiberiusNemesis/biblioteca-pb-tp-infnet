package br.edu.infnet.biblioteca.config;

import br.edu.infnet.biblioteca.model.exception.BookNotFoundException;
import br.edu.infnet.biblioteca.model.exception.DuplicateIsbnException;
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

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(BookNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("book_not_found", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateIsbnException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("duplicate_isbn", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(error("invalid_argument", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
                .toList();
        Map<String, Object> body = error("validation_error", "Request body has invalid fields");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}
