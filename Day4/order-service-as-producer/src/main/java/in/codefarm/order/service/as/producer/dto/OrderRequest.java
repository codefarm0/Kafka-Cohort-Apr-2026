package in.codefarm.order.service.as.producer.dto;

import java.math.BigDecimal;

// Request DTO
    public record OrderRequest(
            String customerId,
            String productId,
            Integer quantity,
            BigDecimal totalAmount
    ) {
    }