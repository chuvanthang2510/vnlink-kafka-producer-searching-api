package vnlink.com.vn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vnlink.com.vn.model.Order;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<Order> orders;
    private long total;
} 