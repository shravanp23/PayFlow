package com.payflow.common;

import com.payflow.payment.InvalidStateTransitionException;
import com.payflow.payment.PaymentNotFoundException;
import com.payflow.refund.RefundException;
import com.payflow.merchant.MerchantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private String requestId() {
        String id = MDC.get("correlationId");
        return id != null ? id : "unknown";
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.of(404, "PAYMENT_NOT_FOUND", ex.getMessage(), req.getRequestURI(), requestId()));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.of(409, "INVALID_PAYMENT_STATE", ex.getMessage(), req.getRequestURI(), requestId()));
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ErrorResponse> handleRefund(RefundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.of(409, "REFUND_NOT_ALLOWED", ex.getMessage(), req.getRequestURI(), requestId()));
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMerchantNotFound(MerchantNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.of(404, "MERCHANT_NOT_FOUND", ex.getMessage(), req.getRequestURI(), requestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.of(400, "VALIDATION_FAILED", message, req.getRequestURI(), requestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        // Never leak internal exception details to the client.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred", req.getRequestURI(), requestId()));
    }
}
