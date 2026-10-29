package com.payflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${payflow.kafka.topics.payment-created}") private String paymentCreated;
    @Value("${payflow.kafka.topics.payment-processing}") private String paymentProcessing;
    @Value("${payflow.kafka.topics.payment-completed}") private String paymentCompleted;
    @Value("${payflow.kafka.topics.payment-failed}") private String paymentFailed;
    @Value("${payflow.kafka.topics.payment-refund}") private String paymentRefund;
    @Value("${payflow.kafka.topics.webhook-events}") private String webhookEvents;
    @Value("${payflow.kafka.topics.payment-dlq}") private String paymentDlq;

    @Bean public NewTopic paymentCreatedTopic() { return TopicBuilder.name(paymentCreated).partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentProcessingTopic() { return TopicBuilder.name(paymentProcessing).partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentCompletedTopic() { return TopicBuilder.name(paymentCompleted).partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentFailedTopic() { return TopicBuilder.name(paymentFailed).partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentRefundTopic() { return TopicBuilder.name(paymentRefund).partitions(3).replicas(1).build(); }
    @Bean public NewTopic webhookEventsTopic() { return TopicBuilder.name(webhookEvents).partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentDlqTopic() { return TopicBuilder.name(paymentDlq).partitions(1).replicas(1).build(); }
}
