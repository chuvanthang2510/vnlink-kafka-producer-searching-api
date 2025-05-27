package vnlink.com.vn.service;

import org.springframework.kafka.core.KafkaTemplate;
import com.google.gson.Gson;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Gson gson = new Gson();

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrder(String orderJson) {
        kafkaTemplate.send("orders", orderJson);
    }
    
    // API batch: nhận danh sách json string
    public void sendOrdersBatch(List<String> ordersJsonList) {
        for (String orderJson : ordersJsonList) {
            sendOrder(orderJson);
        }
    }
}
