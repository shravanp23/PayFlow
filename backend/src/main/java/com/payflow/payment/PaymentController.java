package com.payflow.payment;

import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @Valid @RequestBody CreatePaymentRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        PaymentResponse response = paymentService.createPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> listPayments(
        @RequestParam UUID merchantId,
        @RequestParam(required = false) PaymentStatus status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.listPayments(merchantId, status, pageable));
    }
}
