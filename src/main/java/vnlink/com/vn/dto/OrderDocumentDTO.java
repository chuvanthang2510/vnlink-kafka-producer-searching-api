package vnlink.com.vn.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OrderDocumentDTO {
    private String orderCode;
    private List<String> serviceType;
    private List<String> serviceCode;
    private List<String> serviceId;
    private String serviceMobile;
    private String customerName;
    private List<String> customerMobile;
    private String customerEmail;
    private String customerId;
    private String agentId;
    private String saleChannelId;
    private Date orderDate;
    private String orderStatus;
    private String paymentStatus;
    private String customerCareId;
    private String agentPaymentStatus;
}
