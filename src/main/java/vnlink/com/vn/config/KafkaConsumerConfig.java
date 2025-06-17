package vnlink.com.vn.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Cấu hình cơ bản
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Cấu hình offset và commit
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit để đảm bảo xử lý message thành công
        
        // Cấu hình performance tối ưu cho production
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000); // Tăng batch size
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 phút timeout
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Fetch ít nhất 1KB
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Đợi tối đa 500ms
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800); // 50MB max fetch
        
        // Cấu hình heartbeat và session
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000); // 3 giây
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 giây
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576); // 1MB per partition
        
        // Cấu hình retry và recovery
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1 giây
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);
        
        // Cấu hình bảo mật và monitoring
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "order-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO");
        props.put(ConsumerConfig.CHECK_CRCS_CONFIG, true);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Cấu hình manual commit với batch
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Cấu hình concurrency cho high throughput
        factory.setConcurrency(6); // 6 consumer threads
        
        // Cấu hình error handler với exponential backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (record, exception) -> {
                // Log error và có thể gửi vào DLQ
                System.err.println("Failed to process message after retries: " + record.value());
            },
            new ExponentialBackOff(1000L, 2.0) // Exponential backoff: 1s, 2s, 4s, 8s, 16s...
        );
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.out.println("Retry attempt " + deliveryAttempt + " for record: " + record.value());
        });
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }
}
