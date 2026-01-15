package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data; // Nếu không dùng Lombok thì tự viết Getter/Setter nhé

@Data
public class SePayWebhookDto {
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String subAccount;
    private String content; // QUAN TRỌNG: Đây chính là mã đơn hàng (VD: DH123...)
    private Long transferAmount; // Số tiền khách chuyển
    private String referenceCode;
    private String description;
    private Long id;
}