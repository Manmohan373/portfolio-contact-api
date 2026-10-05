package com.manmohan.contactapi.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.manmohan.contactapi.service.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "success", false, "message", "Please check the form and try again.", "errors", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable() {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Malformed request."));
    }

    @ExceptionHandler(MailService.MailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> mailFailed() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "success", false, "message", "Could not send your message right now. Please email me directly."));
    }
}
