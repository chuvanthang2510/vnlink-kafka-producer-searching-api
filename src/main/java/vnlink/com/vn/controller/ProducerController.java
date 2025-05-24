package vnlink.com.vn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;
import vnlink.com.vn.dto.OrderLog;
import vnlink.com.vn.service.OrderProducer;
import vnlink.com.vn.service.OrderSearchService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/producer")
@RequiredArgsConstructor
public class ProducerController {

    private final OrderProducer producer;
    private final Gson gson = new Gson();
    private final OrderSearchService orderSearchService;

    @PostMapping("/generate-data")
    public ResponseEntity<String> generateData(@RequestParam(defaultValue = "1000000") int numberOfRecords) {
        orderSearchService.generateFakeData(numberOfRecords);
        return ResponseEntity.ok("Data generation completed");
    }

    @PostMapping("/generate-data-multi-thread")
    public ResponseEntity<String> generateDataMultiThread(@RequestParam(defaultValue = "1000000") int numberOfRecords) throws InterruptedException {
        orderSearchService.generateFakeDataMultiThread(numberOfRecords);
        return ResponseEntity.ok("Data generation completed");
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
