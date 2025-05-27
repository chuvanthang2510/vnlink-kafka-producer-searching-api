package vnlink.com.vn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vnlink.com.vn.dto.OrderSearchRequest;
import vnlink.com.vn.dto.OrderSearchResponse;
import vnlink.com.vn.service.OrderDocumentService;
import vnlink.com.vn.service.OrderSearchService;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderDocumentSearchController {

    private final OrderDocumentService orderDocumentService;

    @PostMapping("/search")
    public ResponseEntity<OrderSearchResponse> searchOrders(@Valid @RequestBody OrderSearchRequest request) {
        log.info("Searching orders with request: {}", request);
        OrderSearchResponse response = orderDocumentService.searchOrders(request);
        return ResponseEntity.ok(response);
    }
}
