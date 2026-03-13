package pl.bpiatek.linkshorteneruserservice.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import pl.bpiatek.contracts.user.UserLifecycleEventProto.UserLifecycleEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
class KafkaConfig {

    private final KafkaProperties kafkaProperties;

     KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    AdminClient kafkaAdminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    ProducerFactory<String, UserLifecycleEvent> userLifecycleEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseProducerProperties());
    }

    @Bean
    KafkaTemplate<String, UserLifecycleEvent> enrichedClickEventKafkaTemplate(ObservationRegistry observationRegistry) {
        var template = new KafkaTemplate<>(userLifecycleEventProducerFactory());

        template.setObservationEnabled(true);
        template.setObservationRegistry(observationRegistry);

        return template;
    }

    private Map<String, Object> baseProducerProperties() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true);
        putSchemaRegistry(props);

        return props;
    }

    private void putSchemaRegistry(Map<String, Object> props) {
        var schemaRegistryUrl = kafkaProperties.getProperties().get("schema.registry.url");
        if (schemaRegistryUrl != null) {
            props.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        }
    }
}