package vnlink.com.vn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchResponse {
    private List<OrderDocumentDTO> orders;
    private long total;
}
