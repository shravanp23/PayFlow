package com.payflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * After 3 consumer-level retries, failed messages are published to a dedicated
 * DLQ topic (payment.dlq) rather than being silently dropped or blocking the
 * partition forever. Operators can inspect/replay the DLQ manually.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Value("${payflow.kafka.topics.payment-dlq}")
    private String dlqTopic;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, -1));

        // 3 retries, 2s apart, before giving up and routing to the DLQ.
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
