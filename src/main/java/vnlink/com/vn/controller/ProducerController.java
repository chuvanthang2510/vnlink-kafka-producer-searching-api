package vnlink.com.vn.controller;

import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;
import vnlink.com.vn.dto.OrderLog;
import vnlink.com.vn.service.OrderProducer;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class ProducerController {

    private final OrderProducer producer;
    private final Gson gson = new Gson();

    public ProducerController(OrderProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public String sendOrder(@RequestBody String json) {
        producer.sendOrder(json);
        return "OK";
    }
    
    @PostMapping("/batch")
    public String sendOrdersBatch(@RequestBody List<OrderLog> orders) {
        for (OrderLog order : orders) {
            producer.sendOrder(gson.toJson(order));  // giữ nguyên cách producer nhận String JSON
        }
        return "Batch sent";
    }

    // Hoặc nếu client gửi list object JSON (dùng Gson để convert)
    @PostMapping("/batchObjects")
    public String sendOrdersBatchObjects(@RequestBody List<OrderLog> orders) {
        producer.sendOrdersBatchObjects(orders);
        return "Batch sent " + orders.size() + " orders as objects";
    }
}
