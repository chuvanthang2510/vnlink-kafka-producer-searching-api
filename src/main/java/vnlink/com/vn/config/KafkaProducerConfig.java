package vnlink.com.vn.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka broker địa chỉ, dùng tên container trong Docker network
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");

        // Serializer cho key và value (ở đây dùng String)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Cấu hình tối ưu hiệu năng cho producer:

        // Acks = 1: broker trả lời khi ghi vào leader thành công, nhanh nhưng có rủi ro mất dữ liệu nếu leader chết ngay sau đó
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");

        // Mã hóa nén dữ liệu gửi đi, giảm băng thông và tăng throughput
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Kích thước batch gửi dữ liệu (64KB), tăng hiệu quả gửi
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);

        // Thời gian chờ tối đa (10ms) trước khi gửi batch ngay cả khi chưa đầy batch
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        // Bộ nhớ đệm tổng cho producer (64MB)
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);

        // Số request gửi đồng thời tối đa tới broker trên một kết nối
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Khuyến nghị thêm timeout để tránh treo lâu khi broker không phản hồi
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        // Nếu muốn tăng độ bền, có thể chỉnh retries và delivery.timeout
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        // KafkaTemplate là class giúp gửi message dễ dàng, tự động sử dụng producerFactory config bên trên
        return new KafkaTemplate<>(producerFactory());
    }
}