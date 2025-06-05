package vnlink.com.vn.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class OrderSearchRequest {
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String customerId;
    private String agentId;
    private String saleChannelId;
    private String paymentStatus;
    private String customerCareId;
    private String agentPaymentStatus;
    private String customerMobile;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private String fromCreatedDate;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private String toCreatedDate;

    private Integer page = 0;
    private Integer pageSize = 20;
    private String sortBy = "orderDate";
    private String sortDirection = "DESC";

    private List<ServiceRequest> services;
}