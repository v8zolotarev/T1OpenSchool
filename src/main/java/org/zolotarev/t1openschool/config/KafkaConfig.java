package org.zolotarev.t1openschool.config;

import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import org.zolotarev.t1openschool.dto.TaskDTO;
import org.zolotarev.t1openschool.kafka.KafkaTaskProducer;
import org.zolotarev.t1openschool.kafka.MessageDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.bootstrap-servers}")
    private String servers;

    @Value("${spring.kafka.consumer.session-timeout-ms:150000}")
    private String sessionTimeout;

    @Value("${spring.kafka.consumer.max-partition-fetch-bytes:3000000}")
    private String maxPartitionFetchBytes;

    @Value("${spring.kafka.consumer.max-poll-records:1}")
    private String maxPollRecords;

    @Value("${spring.kafka.consumer.max-poll-interval-ms:3000}")
    private String maxPollIntervalMs;

    @Value("${spring.kafka.topic.task-updates}")
    private String taskTopic;

    @Bean
    public ConsumerFactory<String, TaskDTO> consumerListenerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MessageDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.FALSE);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.zolotarev.t1openschool.dto.TaskDTO");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, MessageDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, MessageDeserializer.class);

        DefaultKafkaConsumerFactory factory = new DefaultKafkaConsumerFactory<String, TaskDTO>(props);
        factory.setKeyDeserializer(new StringDeserializer());

        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TaskDTO> kafkaListenerContainerFactory(
            @Qualifier("consumerListenerFactory") ConsumerFactory<String, TaskDTO> consumerListenerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, TaskDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factoryBuilder(consumerListenerFactory, factory);
        return factory;
    }

    private <T> void factoryBuilder(ConsumerFactory<String, T> consumerFactory, ConcurrentKafkaListenerContainerFactory<String, T> factory) {
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setPollTimeout(5000);
        factory.getContainerProperties().setMicrometerEnabled(true);
        factory.setCommonErrorHandler(errorHandler());
    }

    private CommonErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(1000, 3));
        handler.addNotRetryableExceptions(IllegalStateException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.error("RetryListeners message = {}, offset = {}, deliveryAttempt = {}", ex.getMessage(), record.offset(), deliveryAttempt);
        });
        return handler;
    }

    @Bean("task_status")
    public KafkaTemplate<String, TaskDTO> kafkaTemplate(ProducerFactory<String, TaskDTO> producerTaskFactory) {
        return new KafkaTemplate<>(producerTaskFactory);
    }

    @Bean
    @ConditionalOnProperty(value = "t1.kafka.producer.enable",
            havingValue = "true",
            matchIfMissing = true)
    public KafkaTaskProducer taskProducer(@Qualifier("task_status") KafkaTemplate<String, TaskDTO> kafkaTemplate) {
        kafkaTemplate.setDefaultTopic(taskTopic);
        return new KafkaTaskProducer(kafkaTemplate);
    }

    @Bean
    public ProducerFactory<String, TaskDTO> producerTaskFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

}
