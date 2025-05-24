package vnlink.com.vn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;


@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendToKafka(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    public ListenableFuture<SendResult<String, String>> sendToKafkaMultiThead(String topic, String message) {
        return kafkaTemplate.send(topic, message);
    }
}
