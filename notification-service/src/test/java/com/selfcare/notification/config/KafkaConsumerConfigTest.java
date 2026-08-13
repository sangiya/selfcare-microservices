package com.selfcare.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

class KafkaConsumerConfigTest {

    private final KafkaConsumerConfig config = new KafkaConsumerConfig();

    @Test
    void consumerFactory_setsExpectedKafkaProperties() {
        ConsumerFactory<String, Map<String, Object>> consumerFactory =
                config.consumerFactory("kafka:9092", "notification-service");

        Map<String, Object> props = configurationPropertiesOf(consumerFactory);
        assertThat(props)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "notification-service")
                .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    }

    @Test
    void listenerContainerFactory_usesRecordAckMode() {
        ConsumerFactory<String, Map<String, Object>> consumerFactory =
                config.consumerFactory("kafka:9092", "notification-service");

        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
                config.kafkaListenerContainerFactory(consumerFactory);

        assertThat(factory.getConsumerFactory()).isSameAs(consumerFactory);
        assertThat(factory.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.RECORD);
    }

    private static Map<String, Object> configurationPropertiesOf(
            ConsumerFactory<String, Map<String, Object>> consumerFactory) {
        assertThat(consumerFactory).isInstanceOf(DefaultKafkaConsumerFactory.class);
        return ((DefaultKafkaConsumerFactory<?, ?>) consumerFactory).getConfigurationProperties();
    }
}
