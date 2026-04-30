package in.codefarm.saga.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;
import in.codefarm.saga.event.EventWrapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, EventWrapper<?>> eventWrapperProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        JacksonJsonSerializer<EventWrapper<?>> serializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), serializer);
    }
    
    @Bean
    public KafkaTemplate<String, EventWrapper<?>> eventWrapperKafkaTemplate(
        ProducerFactory<String, EventWrapper<?>> eventWrapperProducerFactory
    ) {
        return new KafkaTemplate<>(eventWrapperProducerFactory);
    }


    @Bean
    public ConsumerFactory<String, EventWrapper<?>> eventWrapperConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        JacksonJsonDeserializer<EventWrapper<?>> deserializer =
                new JacksonJsonDeserializer<>(EventWrapper.class);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventWrapper<?>> eventWrapperKafkaListenerContainerFactory(
            ConsumerFactory<String, EventWrapper<?>> eventWrapperConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, EventWrapper<?>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventWrapperConsumerFactory);
        return factory;
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }

}

