package vnlink.com.vn.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vnlink.com.vn.model.OrderDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderIndexController {

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFromJsonFile(@RequestParam("file") MultipartFile file) {
        int successCount = 0;
        int failureCount = 0;
        List<String> errorLogs = new ArrayList<>();

        try {
            // Đọc nội dung file
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Parse thành danh sách OrderDocument
            List<OrderDocument> orders = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<OrderDocument>>() {}
            );

            List<IndexQuery> queries = new ArrayList<>();
            for (OrderDocument order : orders) {
                try {
                    // Validate từng bản ghi nếu cần (ví dụ: null id, email sai định dạng, ...)
                    if (order.getOrderCode() == null) {
                        throw new IllegalArgumentException("Thiếu id hoặc orderCode");
                    }

                    IndexQuery query = new IndexQueryBuilder()
                            .withId(order.getId())
                            .withObject(order)
                            .build();
                    queries.add(query);
                    successCount++;

                } catch (Exception ex) {
                    failureCount++;
                    errorLogs.add("Lỗi bản ghi ID [" + order.getId() + "]: " + ex.getMessage());
                    log.warn("Lỗi bản ghi ID [{}]: {}", order.getId(), ex.getMessage());
                }
            }

            // Gửi lên Elasticsearch theo batch
            if (!queries.isEmpty()) {
                elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of("orders_1"));
                elasticsearchRestTemplate.indexOps(OrderDocument.class).refresh();
            }

            // Trả về kết quả
            String result = "✅ Thành công: " + successCount +
                    "\n❌ Thất bại: " + failureCount +
                    (errorLogs.isEmpty() ? "" : "\nChi tiết lỗi:\n" + String.join("\n", errorLogs));
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi đọc file: " + e.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống: " + ex.getMessage());
        }
    }
}
