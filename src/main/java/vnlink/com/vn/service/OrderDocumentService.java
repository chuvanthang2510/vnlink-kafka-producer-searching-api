package vnlink.com.vn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import vnlink.com.vn.common.H;
import vnlink.com.vn.dto.OrderSearchRequest;
import vnlink.com.vn.dto.OrderSearchResponse;
import vnlink.com.vn.model.OrderDocument;
import vnlink.com.vn.repository.OrderDocumentRepository;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDocumentService {


    private final ElasticsearchRestTemplate elasticsearchTemplate;

    private static final int MAX_PAGE = 1000;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private void validateRequest(OrderSearchRequest request) {
        if (request.getPage() < 0) {
            request.setPage(0);
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > MAX_PAGE) {
            request.setPageSize(DEFAULT_PAGE_SIZE);
        }
    }

    public OrderSearchResponse searchOrders(OrderSearchRequest request) {
        try {
            // 1. Validate và normalize request
            validateRequest(request);

            // 2. Build query
            NativeSearchQuery searchQuery = buildSearchQuery(request);
            // 3. Thực hiện search trên tất cả index matching pattern
            SearchHits<OrderDocument> searchHits = elasticsearchTemplate.search(
                    searchQuery,
                    OrderDocument.class
            );

            // 5. Map kết quả
            List<OrderDocument> orders = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new OrderSearchResponse(orders, searchHits.getTotalHits());
        } catch (Exception e) {
            log.error("Error searching orders", e);
            throw e;
        }

    }

    private NativeSearchQuery buildSearchQuery(OrderSearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // Điều kiện search chính xác không phân biệt hoa thường
        if (H.isTrue(request.getOrderCode())) {
            boolQuery.must(QueryBuilders.matchQuery("orderCode", request.getOrderCode())); }

        if (H.isTrue(request.getServiceMobile())) {
            boolQuery.must(QueryBuilders.matchQuery("serviceMobile", request.getServiceMobile())); }

        if (H.isTrue(request.getCustomerName())) {
            boolQuery.must(
                    QueryBuilders.matchQuery("customerName", request.getCustomerName())
                            .fuzziness(Fuzziness.AUTO).operator(Operator.AND)  // Bật fuzzy search tự động
            );
        }

        if (H.isTrue(request.getCustomerEmail()))
            boolQuery.must(QueryBuilders.matchQuery("customerEmail", request.getCustomerEmail()));

        if (H.isTrue(request.getCustomerId()))
            boolQuery.must(QueryBuilders.matchQuery("customerId", request.getCustomerId()));

        if (H.isTrue(request.getAgentId()))
            boolQuery.must(QueryBuilders.matchQuery("agentId", request.getAgentId()));

        if (H.isTrue(request.getSaleChannelId()))
            boolQuery.must(QueryBuilders.matchQuery("saleChannelId", request.getSaleChannelId()));

        if (H.isTrue(request.getOrderStatus()))
            boolQuery.must(QueryBuilders.matchQuery("orderStatus", request.getOrderStatus()));

        if (H.isTrue(request.getPaymentStatus()))
            boolQuery.must(QueryBuilders.matchQuery("paymentStatus", request.getPaymentStatus()));

        if (H.isTrue(request.getCustomerCareId()))
            boolQuery.must(QueryBuilders.matchQuery("customerCareId", request.getCustomerCareId()));

        if (H.isTrue(request.getAgentPaymentStatus()))
            boolQuery.must(QueryBuilders.matchQuery("agentPaymentStatus", request.getAgentPaymentStatus()));


        // List fields - Phân biệt chũ hoa chữ thường các phần tử trong mảng
        if (H.isTrue(request.getServiceType()))
            boolQuery.must(QueryBuilders.termsQuery("serviceType.raw", request.getServiceType()));

        if (H.isTrue(request.getServiceCode()))
            boolQuery.must(QueryBuilders.termsQuery("serviceCode.raw", request.getServiceCode()));

        if (H.isTrue(request.getServiceId()))
            boolQuery.must(QueryBuilders.termsQuery("serviceId.raw", request.getServiceId()));

        if (H.isTrue(request.getCustomerMobile()))
            boolQuery.must(QueryBuilders.termsQuery("customerMobile.raw", request.getCustomerMobile()));

        if (H.isTrue(request.getSubService()))
            boolQuery.must(QueryBuilders.termsQuery("subService.raw", request.getSubService()));

        // Tìm theo khoảng thời gian
        RangeQueryBuilder dateRangeQuery = QueryBuilders.rangeQuery("orderDate");
        if (H.isTrue(request.getFromCreatedDate())) {
            dateRangeQuery.gte(request.getFromCreatedDate());
        }
        if (H.isTrue(request.getToCreatedDate())) {
            dateRangeQuery.lte(request.getToCreatedDate());
        }
        boolQuery.must(dateRangeQuery);


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
}