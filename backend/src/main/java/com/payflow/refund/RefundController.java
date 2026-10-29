package com.payflow.refund;

import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.refund.dto.RefundResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/{paymentId}/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<RefundResponse> createRefund(
        @PathVariable String paymentId,
        @Valid @RequestBody(required = false) CreateRefundRequest request
    ) {
        CreateRefundRequest body = request != null ? request : new CreateRefundRequest(null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.createRefund(paymentId, body));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','VIEWER')")
    public List<RefundResponse> listRefunds(@PathVariable String paymentId) {
        return refundService.listRefundsForPayment(paymentId);
    }
}
