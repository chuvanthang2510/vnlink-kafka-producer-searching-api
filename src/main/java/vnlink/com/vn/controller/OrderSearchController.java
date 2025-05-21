package vnlink.com.vn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vnlink.com.vn.dto.SearchRequestMultiField;
import vnlink.com.vn.dto.SearchResponse;
import vnlink.com.vn.service.OrderSearchService;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderSearchController {
    private final OrderSearchService orderSearchService;

    @GetMapping("/searchMultiField")
    public ResponseEntity<SearchResponse> searchOrders(@RequestBody SearchRequestMultiField request) {
        try {
            log.debug("Searching with request: {}", request);
            SearchResponse response = orderSearchService.searchOrdersMultiField(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching orders", e);
            return ResponseEntity.badRequest().build();
        }
    }

} 