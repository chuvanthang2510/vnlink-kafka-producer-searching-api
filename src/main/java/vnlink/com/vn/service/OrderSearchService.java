package vnlink.com.vn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import vnlink.com.vn.common.H;
import vnlink.com.vn.dto.SearchRequestMultiField;
import vnlink.com.vn.dto.SearchResponse;
import vnlink.com.vn.model.Order;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSearchService {

    private static final String INDEX_PATTERN = "orders_v2*";
    private static final int MAX_PAGE = 1000;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ElasticsearchRestTemplate elasticsearchTemplate;
    private final RestHighLevelClient restHighLevelClient;

    public SearchResponse searchOrdersMultiField(SearchRequestMultiField request) {
        try {
            // 1. Validate và normalize request
            validateRequest(request);
            
            // 2. Build query
            NativeSearchQuery searchQuery = buildSearchQuery(request);
            
            // 3. Thực hiện search trên tất cả index matching pattern
            SearchHits<Order> searchHits = elasticsearchTemplate.search(
                searchQuery, 
                Order.class
            );
            
            // 4. Log thông tin search nếu debug
            if (log.isDebugEnabled()) {
                log.debug("Searching indices matching pattern: {}", INDEX_PATTERN);
                log.debug("Search query: {}", searchQuery.getQuery().toString());
                log.debug("Total hits: {}", searchHits.getTotalHits());
            }
            
            // 5. Map kết quả
            List<Order> orders = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
                    
            return new SearchResponse(orders, searchHits.getTotalHits());
            
        } catch (Exception e) {
            log.error("Error searching orders", e);
            throw e;
        }
    }

    /**
     * Validate và normalize request
     */
    private void validateRequest(SearchRequestMultiField request) {
        if (request.getPage() < 0) {
            request.setPage(0);
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > MAX_PAGE) {
            request.setPageSize(DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * Build search query
     */
    private NativeSearchQuery buildSearchQuery(SearchRequestMultiField request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Thêm các điều kiện tìm kiếm
        if (H.isTrue(request.getId())) {
            boolQuery.must(QueryBuilders.termQuery("_id", request.getId()));
        }

        if (H.isTrue(request.getCode())) {
            boolQuery.must(QueryBuilders.matchQuery("code", request.getCode())
                .boost(2.0f));  // Boost cho code
        }

        if (H.isTrue(request.getBookingCode())) {
            boolQuery.must(QueryBuilders.matchQuery("bookingCode", request.getBookingCode())
                .boost(2.0f));  // Boost cho booking code
        }

        if (H.isTrue(request.getPhoneNumber())) {
            boolQuery.must(QueryBuilders.matchQuery("phoneNumber", request.getPhoneNumber()));
        }

        if (H.isTrue(request.getCustomerName())) {
            boolQuery.must(QueryBuilders.matchQuery("customerName", request.getCustomerName())
                .boost(1.5f));  // Boost cho tên khách hàng
        }

        if (H.isTrue(request.getCustomerEmail())) {
            boolQuery.must(QueryBuilders.termQuery("customerEmail.raw", 
                request.getCustomerEmail().toLowerCase()));
        }

        // Tìm theo khoảng thời gian
        if (H.isTrue(request.getFromCreatedDate()) || H.isTrue(request.getToCreatedDate())) {
            RangeQueryBuilder dateRangeQuery = QueryBuilders.rangeQuery("orderDate")
                    .gte(request.getFromCreatedDate())
                    .lte(request.getToCreatedDate());
            boolQuery.must(dateRangeQuery);
        }

        // Build search query với các tối ưu
        return new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(request.getPage(), request.getPageSize()))
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("orderDate").order(SortOrder.DESC))
                // Thêm preference để đảm bảo kết quả nhất quán
                .withPreference("_local")
                // Thêm timeout
                .withTimeout(Duration.ofSeconds(30))
                // Thêm track_total_hits
                .withTrackTotalHits(true)
                .build();
    }

    public void generateFakeData(int numberOfRecords) {
        log.info("Starting to generate {} fake records", numberOfRecords);

        // Danh sách các giá trị mẫu
        String[] firstNames = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng"};
        String[] middleNames = {"Văn", "Thị", "Hoàng", "Đức", "Minh", "Hữu", "Công", "Đình", "Xuân", "Hồng"};
        String[] lastNames = {"An", "Bình", "Cường", "Dũng", "Em", "Phúc", "Giang", "Hùng", "Khang", "Linh"};
        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "company.com"};
        String[] statuses = {"PENDING", "CONFIRMED", "CANCELLED", "COMPLETED", "PROCESSING"};

        // Số lượng bản ghi mỗi lần bulk
        int batchSize = 10000;
        int totalBatches = (int) Math.ceil((double) numberOfRecords / batchSize);

        AtomicInteger counter = new AtomicInteger(0);
        Random random = new Random();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (int batch = 0; batch < totalBatches; batch++) {
            BulkRequest bulkRequest = new BulkRequest();
            int currentBatchSize = Math.min(batchSize, numberOfRecords - (batch * batchSize));

            for (int i = 0; i < currentBatchSize; i++) {
                // Tạo dữ liệu ngẫu nhiên
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String middleName = middleNames[random.nextInt(middleNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                String customerName = String.format("%s %s %s", firstName, middleName, lastName);

                String username = (firstName + lastName).toLowerCase().replaceAll("\\s+", "");
                username = removeVietnameseDiacritics(username);
                String domain = domains[random.nextInt(domains.length)];
                String customerEmail = String.format("%s@%s", username, domain);

                String code = String.format("ORD%08d", counter.incrementAndGet());
                String bookingCode = String.format("BK%d%06d",
                        Calendar.getInstance().get(Calendar.YEAR),
                        random.nextInt(1000000));
                String phoneNumber = String.format("09%d", 10000000 + random.nextInt(90000000));

                // Tạo ngày ngẫu nhiên trong 2 năm gần đây
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.YEAR, -2);
                long startTime = cal.getTimeInMillis();
                long endTime = System.currentTimeMillis();
                long randomTime = startTime + (long)(random.nextDouble() * (endTime - startTime));
                Date orderDate = new Date(randomTime);

                double totalAmount = 100000 + random.nextDouble() * 9000000;
                String status = statuses[random.nextInt(statuses.length)];

                // Tạo JSON document
                Map<String, Object> document = new HashMap<>();
                document.put("id", UUID.randomUUID().toString());
                document.put("code", code);
                document.put("bookingCode", bookingCode);
                document.put("phoneNumber", phoneNumber);
                document.put("customerName", customerName);
                document.put("customerEmail", customerEmail);
                document.put("orderDate", sdf.format(orderDate));
                document.put("totalAmount", totalAmount);
                document.put("status", status);

                // Thêm vào bulk request
                IndexRequest indexRequest = new IndexRequest("orders_v2_0521205")
                        .source(document, XContentType.JSON);
                bulkRequest.add(indexRequest);
            }

            try {
                BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                if (bulkResponse.hasFailures()) {
                    log.error("Bulk request failed: {}", bulkResponse.buildFailureMessage());
                } else {
                    log.info("Successfully indexed batch {}/{} ({} records)",
                            batch + 1, totalBatches, currentBatchSize);
                }
            } catch (Exception e) {
                log.error("Error during bulk indexing: ", e);
            }
        }

        log.info("Finished generating {} fake records", numberOfRecords);
    }

    private String removeVietnameseDiacritics(String str) {
        str = str.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        str = str.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        str = str.replaceAll("[ìíịỉĩ]", "i");
        str = str.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        str = str.replaceAll("[ùúụủũưừứựửữ]", "u");
        str = str.replaceAll("[ỳýỵỷỹ]", "y");
        str = str.replaceAll("[đ]", "d");
        str = str.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        str = str.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        str = str.replaceAll("[ÌÍỊỈĨ]", "I");
        str = str.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        str = str.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        str = str.replaceAll("[ỲÝỴỶỸ]", "Y");
        str = str.replaceAll("[Đ]", "D");
        return str;
    }
} 