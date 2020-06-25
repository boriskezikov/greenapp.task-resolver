package com.task.resolver.configuration;

import lombok.Setter;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties
public class KafkaConfiguration {

    @Bean
    @ConfigurationProperties("kafka")
    KafkaProperties kafkaProperties() {
        return new KafkaProperties();
    }

    @Bean
    @RefreshScope
    public KafkaReceiver<String, String> kafkaReceiver(@Value("${kafka.topic}") String topic, KafkaProperties kafkaProperties) {
        var producerOptions = ReceiverOptions.<String, String>create(kafkaProperties.properties)
            .subscription(List.of(topic))
            .withValueDeserializer(new StringDeserializer())
            .withKeyDeserializer(new StringDeserializer());
        return KafkaReceiver.create(producerOptions);
    }

    @Setter
    public static class KafkaProperties {

        public Map<String, Object> properties = new HashMap<>();
    }
}
