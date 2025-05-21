package vnlink.com.vn.dto;

import lombok.Data;

import java.util.Date;

@Data
public class OrderLog {
    private String id;
    private String code;
    private String bookingCode;
    private String phoneNumber;
    private String customerName;
    private String customerEmail;
    private Date orderDate;
    private double totalAmount;
    private String status;
}
