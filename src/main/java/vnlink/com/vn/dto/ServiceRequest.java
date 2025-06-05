package vnlink.com.vn.dto;

import lombok.Data;

@Data
public class ServiceRequest {
    private String id;
    private String serviceId;
    private String serviceCode;
    private String code;
    private String subService;
}
