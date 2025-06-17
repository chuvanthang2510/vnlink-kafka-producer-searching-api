package vnlink.com.vn.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import vnlink.com.vn.common.H;
import vnlink.com.vn.model.OrderDocument;
import vnlink.com.vn.model.ServiceInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaConsumer {

    private final ElasticsearchRestTemplate elasticsearchTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Circuit breaker và monitoring
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalUpserts = new AtomicLong(0);
    private final AtomicLong totalInserts = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    private static final int MAX_CONSECUTIVE_FAILURES = 10;
    private static final long CIRCUIT_BREAKER_RESET_TIME = 60000; // 1 phút
    private volatile long lastFailureTime = 0;
    private static final DateTimeFormatter INDEX_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @KafkaListener(topics = "${spring.kafka.consumer.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment ack) {
        long startTime = System.currentTimeMillis();

        try {
            if (isCircuitBreakerOpen()) {
                log.warn("Circuit breaker is open, skipping message processing");
                ack.acknowledge();
                return;
            }

            JsonNode node = objectMapper.readTree(message);
            String orderCode = getSafeText(node, "orderCode");

            if (!H.isTrue(orderCode)) {
                log.warn("Invalid or missing orderCode, skipping message.");
                ack.acknowledge();
                return;
            }

            OrderDocument order = buildOrderDocument(node, orderCode);
            String indexName = getIndexName();

            // Thực hiện upsert với retry logic
            boolean isUpdated = performUpsertWithRetry(order, indexName);

            // Update metrics
            totalProcessed.incrementAndGet();
            if (isUpdated) {
                totalUpserts.incrementAndGet();
            } else {
                totalInserts.incrementAndGet();
            }

            // Reset circuit breaker
            consecutiveFailures.set(0);
            ack.acknowledge();

            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);

            // Log performance metrics
            if (totalProcessed.get() % 100 == 0) {
                log.info("Performance metrics - Processed: {}, Avg time: {}ms, Success rate: {}%",
                        totalProcessed.get(),
                        totalProcessingTime.get() / totalProcessed.get(),
                        calculateSuccessRate());
            }

            log.debug("Processed order {} in {}ms ({}), index: {}",
                    orderCode, processingTime, isUpdated ? "UPDATED" : "INSERTED", indexName);

        } catch (Exception e) {
            handleError(e, receivedTopic, offset, startTime);
            throw new RuntimeException("Failed to process message", e);
        }
    }

    private OrderDocument buildOrderDocument(JsonNode node, String orderCode) throws Exception {
        OrderDocument order = new OrderDocument();
        order.setOrderCode(orderCode);
        order.setId(orderCode);

        Date createdDate = H.isTrue(node.get("createdDate"))
                ? parseCreatedDate(node.get("createdDate").asText())
                : new Date();
        order.setCreatedDate(createdDate);


        // Mapping các field chính
        order.setAgentId(H.isTrue(node.get("agentId")) ? node.get("agentId").asText() : null);
        order.setSaleChannelId(H.isTrue(node.get("saleChannelId")) ? node.get("saleChannelId").asText() : null);
        order.setCustomerId(H.isTrue(node.get("customerId")) ? node.get("customerId").asText() : null);
        order.setAgentPaymentStatus(H.isTrue(node.get("agentPaymentStatus")) ? node.get("agentPaymentStatus").asText() : null);
        order.setPaymentStatus(H.isTrue(node.get("paymentStatus")) ? node.get("paymentStatus").asText() : null);
        order.setCustomerName(H.isTrue(node.get("customerName")) ? node.get("customerName").asText() : null);
        order.setCustomerEmail(H.isTrue(node.get("customerEmail")) ? node.get("customerEmail").asText() : null);
        order.setCustomerMobile(H.isTrue(node.get("customerMobile")) ? node.get("customerMobile").asText() : null);
        order.setCustomerCareId(H.isTrue(node.get("customerCareId")) ? node.get("customerCareId").asText() : "customerCareId");

        String email = order.getCustomerEmail();
        order.setEmailPrefix(email != null && email.contains("@") ? email.split("@")[0] : "");

        // Mapping danh sách services
        if (node.has("services") && node.get("services").isArray()) {
            List<ServiceInfo> services = new ArrayList<>();
            for (JsonNode s : node.get("services")) {
                ServiceInfo info = new ServiceInfo();
                info.setCode(H.isTrue(s.get("code")) ? s.get("code").asText() : null);
                info.setServiceCode(H.isTrue(s.get("serviceCode")) ? s.get("serviceCode").asText() : null);
                info.setId(H.isTrue(s.get("id")) ? s.get("id").asText() : null);
                info.setServiceId(H.isTrue(s.get("serviceId")) ? s.get("serviceId").asText() : "serviceId");
                info.setSubService(H.isTrue(s.get("subService")) ? s.get("subService").asText() : "subService");
                services.add(info);
            }
            order.setServices(services);
        }

        return order;
    }

    private Date parseCreatedDate(String createdRaw) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy hh.mm.ss.nnnnnnnnn a", Locale.ENGLISH);
            LocalDateTime ldt = LocalDateTime.parse(createdRaw, formatter);

            // Dùng múi giờ Việt Nam thay vì systemDefault()
            ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");

            return Date.from(ldt.atZone(vietnamZone).toInstant());
        } catch (Exception ex) {
            log.warn("Invalid createdDate '{}'. Using current time.", createdRaw);
            return new Date();
        }
    }


    private String getIndexName() {
        return "orders_" + LocalDate.now().format(INDEX_DATE_FORMATTER);
    }

    private boolean performUpsertWithRetry(OrderDocument order, String defaultIndexName) {
        try {
            // 1. Tìm index chứa document hiện tại (nếu có)
            String existingIndex = findIndexByOrderCode(order.getOrderCode());

            // 2. Nếu tìm thấy → ghi vào index cũ, ngược lại ghi vào index hiện tại
            String targetIndex = (H.isTrue(existingIndex)) ? existingIndex : defaultIndexName;
            IndexCoordinates writeIndex = IndexCoordinates.of(targetIndex);

            // 3. Đảm bảo index tồn tại
            ensureIndexExists(writeIndex);

            // 4. Ghi dữ liệu
            if (existingIndex != null) {
                // UPDATE
                elasticsearchTemplate.save(order, writeIndex);
                return true;
            } else {
                // INSERT
                IndexQuery query = new IndexQuery();
                query.setId(order.getOrderCode());
                query.setObject(order);
                elasticsearchTemplate.index(query, writeIndex);
                return false;
            }

        } catch (Exception e) {
            try {
                Thread.sleep(100); // Exponential backoff
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during retry", ie);
            }
        }

        return false;
    }

    private String findIndexByOrderCode(String orderCode) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.idsQuery().addIds(orderCode))
                .withStoredFields("_index")
                .build();

        IndexCoordinates searchIndices = IndexCoordinates.of("orders*");

        SearchHits<OrderDocument> hits = elasticsearchTemplate.search(query, OrderDocument.class, searchIndices);

        if (hits.hasSearchHits()) {
            return hits.getSearchHit(0).getIndex(); // Trả về index đầu tiên tìm thấy
        }
        return null;
    }


    private void ensureIndexExists(IndexCoordinates indexCoordinates) {
        IndexOperations indexOps = elasticsearchTemplate.indexOps(indexCoordinates);
        if (!indexOps.exists()) {
            log.info("Creating index: {}", indexCoordinates.getIndexName());
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping(OrderDocument.class));
        }
    }


    private void handleError(Exception e, String topic, long offset, long startTime) {
        int failures = consecutiveFailures.incrementAndGet();
        totalErrors.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();

        long processingTime = System.currentTimeMillis() - startTime;

        log.error("Error processing message from topic: {}, offset: {} in {}ms. Error: {}",
                topic, offset, processingTime, e.getMessage(), e);

        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            log.error("Circuit breaker activated after {} consecutive failures", failures);
        }
    }

    private boolean isCircuitBreakerOpen() {
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            if (System.currentTimeMillis() - lastFailureTime > CIRCUIT_BREAKER_RESET_TIME) {
                log.info("Circuit breaker reset timeout reached, resetting counter");
                consecutiveFailures.set(0);
                return false;
            }
            return true;
        }
        return false;
    }

    private String getSafeText(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : null;
    }

    private double calculateSuccessRate() {
        long total = totalProcessed.get();
        long errors = totalErrors.get();
        return total > 0 ? ((double) (total - errors) / total) * 100 : 0;
    }
}