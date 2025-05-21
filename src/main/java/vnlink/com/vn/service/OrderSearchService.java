package vnlink.com.vn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
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
import vnlink.com.vn.repository.OrderRepository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSearchService {

    private static final int MAX_PAGE_SIZE = 1000;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_TOTAL_RESULTS = 1000;
    private static final int SCROLL_SIZE = 5000;
    private static final long SCROLL_TIMEOUT = 60000; // 1 minute

    private final OrderRepository orderRepository;
    private final ElasticsearchRestTemplate elasticsearchTemplate;
    private final RestHighLevelClient restHighLevelClient;

    public SearchResponse searchOrdersMultiField(SearchRequestMultiField request) {
        int page = Math.max(0, request.getPage());
        int size = validateAndNormalizeSize(request.getPageSize());

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Tìm theo từng trường nếu có giá trị
        if (H.isTrue(request.getId())) {
            boolQuery.must(QueryBuilders.matchQuery("_id", request.getId()));
        }

        if (H.isTrue(request.getCode())) {
            boolQuery.must(QueryBuilders.matchQuery("code", request.getCode()));
        }

        if (H.isTrue(request.getBookingCode())) {
            boolQuery.must(QueryBuilders.matchQuery("bookingCode", request.getBookingCode()));
        }

        if (H.isTrue(request.getPhoneNumber())) {
            boolQuery.must(QueryBuilders.matchQuery("phoneNumber", request.getPhoneNumber()));
        }

        if (H.isTrue(request.getCustomerName())) {
            boolQuery.must(QueryBuilders.matchQuery("customerName", request.getCustomerName()));
        }

        if (H.isTrue(request.getCustomerEmail())) {
            // Có thể dùng match hoặc term nếu muốn chính xác email
            boolQuery.must(QueryBuilders.termQuery("customerEmail.raw", request.getCustomerEmail().toLowerCase()));
        }

        // Tìm theo khoảng thời gian
        if (H.isTrue(request.getFromCreatedDate()) || H.isTrue(request.getToCreatedDate())) {
            RangeQueryBuilder dateRangeQuery = QueryBuilders.rangeQuery("orderDate");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            if (H.isTrue(request.getFromCreatedDate())) {
                dateRangeQuery.gte(sdf.format(request.getFromCreatedDate()));
            }
            if (H.isTrue(request.getToCreatedDate())) {
                dateRangeQuery.lte(sdf.format(request.getToCreatedDate()));
            }

            boolQuery.must(dateRangeQuery);
        }


        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(page, size))
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("orderDate").order(SortOrder.DESC))
                .build();


        log.debug("Search query: {}", searchQuery.getQuery().toString());

        SearchHits<Order> searchHits = elasticsearchTemplate.search(searchQuery, Order.class);

        List<Order> orders = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        long total = searchHits.getTotalHits();

        return new SearchResponse(orders, total);
    }

    private int validateAndNormalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
} 