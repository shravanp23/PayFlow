package com.payflow.refund;

import com.payflow.payment.Payment;
import com.payflow.payment.PaymentRepository;
import com.payflow.payment.PaymentStateMachine;
import com.payflow.payment.PaymentStatus;
import com.payflow.provider.PaymentProvider;
import com.payflow.provider.ProviderRegistry;
import com.payflow.provider.ProviderResult;
import com.payflow.refund.dto.CreateRefundRequest;
import com.payflow.webhook.WebhookEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock private RefundRepository refundRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProviderRegistry providerRegistry;
    @Mock private PaymentProvider paymentProvider;
    @Mock private WebhookEventPublisher webhookEventPublisher;

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();
    private RefundService refundService;

    private Payment successPayment;

    @BeforeEach
    void setUp() throws Exception {
        refundService = new RefundService(refundRepository, paymentRepository, stateMachine, providerRegistry, webhookEventPublisher);

        successPayment = Payment.create("PAY_TEST01", new BigDecimal("1000.00"), "INR",
            java.util.UUID.randomUUID(), "UPI", "MockUPIProvider", null, null, null);
        setStatus(successPayment, PaymentStatus.SUCCESS);

        when(paymentRepository.findById("PAY_TEST01")).thenReturn(Optional.of(successPayment));
        when(providerRegistry.resolve(anyString())).thenReturn(paymentProvider);
    }

    private void setStatus(Payment payment, PaymentStatus status) throws Exception {
        Field f = Payment.class.getDeclaredField("status");
        f.setAccessible(true);
        f.set(payment, status);
    }

    @Test
    void fullRefundSucceeds() {
        when(refundRepository.sumActiveRefundedAmount("PAY_TEST01")).thenReturn(BigDecimal.ZERO);
        when(paymentProvider.refund(any(), any(), any())).thenReturn(ProviderResult.success("RFD_REF1"));
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = refundService.createRefund("PAY_TEST01", new CreateRefundRequest(null, "customer request"));

        assertThat(response.amount()).isEqualByComparingTo("1000.00");
        assertThat(response.status()).isEqualTo(RefundStatus.REFUNDED);
        assertThat(successPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void partialRefundSucceedsAndPaymentStaysInRefundProcessingFlow() {
        when(refundRepository.sumActiveRefundedAmount("PAY_TEST01")).thenReturn(BigDecimal.ZERO);
        when(paymentProvider.refund(any(), any(), any())).thenReturn(ProviderResult.success("RFD_REF1"));
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = refundService.createRefund("PAY_TEST01", new CreateRefundRequest(new BigDecimal("400.00"), "partial"));

        assertThat(response.amount()).isEqualByComparingTo("400.00");
        assertThat(response.status()).isEqualTo(RefundStatus.REFUNDED);
    }

    @Test
    void overRefundIsRejected() {
        when(refundRepository.sumActiveRefundedAmount("PAY_TEST01")).thenReturn(new BigDecimal("900.00"));

        assertThatThrownBy(() ->
            refundService.createRefund("PAY_TEST01", new CreateRefundRequest(new BigDecimal("500.00"), "too much"))
        ).isInstanceOf(RefundException.class).hasMessageContaining("exceeds remaining refundable amount");
    }

    @Test
    void refundOnNonSuccessPaymentIsRejected() throws Exception {
        setStatus(successPayment, PaymentStatus.FAILED);

        assertThatThrownBy(() ->
            refundService.createRefund("PAY_TEST01", new CreateRefundRequest(null, "invalid"))
        ).isInstanceOf(RefundException.class).hasMessageContaining("cannot be refunded");
    }

    @Test
    void fullyRefundedPaymentRejectsFurtherRefunds() {
        when(refundRepository.sumActiveRefundedAmount("PAY_TEST01")).thenReturn(new BigDecimal("1000.00"));

        assertThatThrownBy(() ->
            refundService.createRefund("PAY_TEST01", new CreateRefundRequest(null, "duplicate"))
        ).isInstanceOf(RefundException.class).hasMessageContaining("Nothing left to refund");
    }
}
