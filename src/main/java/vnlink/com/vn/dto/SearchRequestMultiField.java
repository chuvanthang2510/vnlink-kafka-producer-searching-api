package vnlink.com.vn.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class SearchRequestMultiField {
    private String id;
    private String code;
    private String bookingCode;
    private String phoneNumber;
    private String customerName;
    private String customerEmail;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private String fromCreatedDate;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private String toCreatedDate;

    private int page = 0;
    private int pageSize = 20;
}
