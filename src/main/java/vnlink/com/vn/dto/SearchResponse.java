package vnlink.com.vn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import vnlink.com.vn.model.Order;

import java.util.List;


@Data
@AllArgsConstructor
public class SearchResponse {
    private List<Order> orders;
    private long total;
} 