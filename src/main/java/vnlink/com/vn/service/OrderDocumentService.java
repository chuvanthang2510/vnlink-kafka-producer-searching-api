package vnlink.com.vn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import vnlink.com.vn.common.H;
import vnlink.com.vn.dto.OrderDocumentDTO;
import vnlink.com.vn.dto.OrderSearchRequest;
import vnlink.com.vn.dto.OrderSearchResponse;
import vnlink.com.vn.dto.ServiceRequest;
import vnlink.com.vn.model.OrderDocument;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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
            validateRequest(request);
            NativeSearchQuery searchQuery = buildSearchQuery(request);

            SearchHits<OrderDocument> searchHits = elasticsearchTemplate.search(
                    searchQuery,
                    OrderDocument.class
            );

            List<OrderDocumentDTO> orders = searchHits.getSearchHits().stream()
                    .map(hit -> {
                        OrderDocument doc = hit.getContent();
                        return new OrderDocumentDTO(doc.getOrderCode(), doc.getCustomerName());
                    })
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

        if (H.isTrue(request.getCustomerName())) {
            boolQuery.must(
                    QueryBuilders.matchQuery("customerName", request.getCustomerName())
                            .fuzziness(Fuzziness.AUTO).operator(Operator.AND)  // Bật fuzzy search tự động
            );
        }

        if (H.isTrue(request.getCustomerEmail())) {
            String input = request.getCustomerEmail().trim();

            if (!input.contains("@")) {
                // Tìm tiền tố emailPrefix khi user chỉ nhập phần trước @
                boolQuery.must(QueryBuilders.prefixQuery("prefixEmail.raw", input));
            } else {
                // Tìm chính xác email đầy đủ với trường customerEmail.raw (keyword)
                boolQuery.must(QueryBuilders.termQuery("customerEmail.raw", input));
            }
        }

        if (H.isTrue(request.getCustomerId()))
            boolQuery.must(QueryBuilders.matchQuery("customerId", request.getCustomerId()));

        if (H.isTrue(request.getAgentId()))
            boolQuery.must(QueryBuilders.matchQuery("agentId", request.getAgentId()));

        if (H.isTrue(request.getSaleChannelId()))
            boolQuery.must(QueryBuilders.matchQuery("saleChannelId", request.getSaleChannelId()));

        if (H.isTrue(request.getPaymentStatus()))
            boolQuery.must(QueryBuilders.matchQuery("paymentStatus", request.getPaymentStatus()));

        if (H.isTrue(request.getCustomerCareId()))
            boolQuery.must(QueryBuilders.matchQuery("customerCareId", request.getCustomerCareId()));

        if (H.isTrue(request.getAgentPaymentStatus()))
            boolQuery.must(QueryBuilders.matchQuery("agentPaymentStatus", request.getAgentPaymentStatus()));



        if (H.isTrue(request.getCustomerMobile())) {
            String normalizedPhone = normalizePhone(request.getCustomerMobile().trim());
            boolQuery.must(QueryBuilders.termsQuery("customerMobile.raw", normalizedPhone));
        }

        // Nested serviceRequests
        if (H.isTrue(request.getServices())) {
            List<QueryBuilder> nestedQueries = new ArrayList<>();
            for (ServiceRequest sr : request.getServices()) {
                BoolQueryBuilder nestedBool = QueryBuilders.boolQuery();

                if (H.isTrue(sr.getId()))
                    nestedBool.filter(QueryBuilders.termQuery("services.id.raw", sr.getId()));

                if (H.isTrue(sr.getServiceId()))
                    nestedBool.filter(QueryBuilders.termQuery("services.serviceId.raw", sr.getServiceId()));

                if (H.isTrue(sr.getServiceCode()))
                    nestedBool.filter(QueryBuilders.termQuery("services.serviceCode.raw", sr.getServiceCode()));

                if (H.isTrue(sr.getCode()))
                    nestedBool.filter(QueryBuilders.termQuery("services.code.raw", sr.getCode()));

                if (H.isTrue(sr.getSubService()))
                    nestedBool.filter(QueryBuilders.termQuery("services.subService.raw", sr.getSubService()));

                if (!nestedBool.must().isEmpty() || !nestedBool.filter().isEmpty()) {
                    nestedQueries.add(QueryBuilders.nestedQuery("services", nestedBool, ScoreMode.None));
                }
            }

            // Nếu muốn match ít nhất 1 service, dùng should
            if (!nestedQueries.isEmpty()) {
                BoolQueryBuilder nestedShould = QueryBuilders.boolQuery();
                nestedQueries.forEach(nestedShould::should);
                nestedShould.minimumShouldMatch(1); // bắt buộc ít nhất 1 khớp
                boolQuery.must(nestedShould);
            }
        }

        // Khoảng thời gian
        if (H.isTrue(request.getFromCreatedDate()) || H.isTrue(request.getToCreatedDate())) {
            RangeQueryBuilder dateRangeQuery = QueryBuilders.rangeQuery("createdDate");
            if (H.isTrue(request.getFromCreatedDate())) {
                dateRangeQuery.gte(request.getFromCreatedDate());
            }
            if (H.isTrue(request.getToCreatedDate())) {
                dateRangeQuery.lte(request.getToCreatedDate());
            }
            boolQuery.filter(dateRangeQuery);
        }


        // Build search query với các tối ưu
        return new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(request.getPage(), request.getPageSize()))
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createdDate").order(SortOrder.DESC))
                // Thêm preference để đảm bảo kết quả nhất quán
                .withPreference("_local")
                // Thêm timeout
                .withTimeout(Duration.ofSeconds(30))
                // Thêm track_total_hits
                .withTrackTotalHits(true)
                .build();
    }

    public  String normalizePhone(String input) {
        if (input == null) return null;
        input = input.trim().replaceAll("[^0-9]", "");
        if (input.startsWith("84")) {
            return "0" + input.substring(2);
        }
        if (input.startsWith("0")) {
            return input;
        }
        return input;
    }
}