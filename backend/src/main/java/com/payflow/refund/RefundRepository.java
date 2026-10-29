package com.payflow.refund;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, java.util.UUID> {

    Optional<Refund> findByRefundId(String refundId);

    List<Refund> findByPaymentId(String paymentId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId " +
           "AND r.status IN ('REFUNDED', 'REFUND_PROCESSING', 'REFUND_REQUESTED')")
    BigDecimal sumActiveRefundedAmount(@Param("paymentId") String paymentId);
}
