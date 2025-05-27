package vnlink.com.vn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vnlink.com.vn.model.OrderDocument;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchResponse {
    private List<OrderDocument> orders;
    private long total;
}
