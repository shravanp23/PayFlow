package com.payflow.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    Page<Payment> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Payment> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status, Pageable pageable);
}
