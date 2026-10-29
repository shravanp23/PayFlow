package com.payflow.payment.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payflow.kafka.topics.payment-created}")
    private String paymentCreatedTopic;

    /**
     * Fires only after the surrounding DB transaction commits successfully.
     * If the transaction rolls back, this never runs — no Kafka message is
     * ever sent for a payment that doesn't actually exist in Postgres.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCreated(PaymentCreatedApplicationEvent event) {
        PaymentCreatedEvent payload = event.payload();
        var message = MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, paymentCreatedTopic)
            .setHeader(KafkaHeaders.KEY, payload.paymentId())
            .setHeader("correlationId", payload.correlationId() != null ? payload.correlationId() : "unknown")
            .build();

        kafkaTemplate.send(message);
        log.info("Published payment.created for paymentId={} correlationId={}", payload.paymentId(), payload.correlationId());
    }
}
