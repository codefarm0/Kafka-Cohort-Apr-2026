package in.codefarm.order.service.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {
    private Long userId;
    private Long courseId;
    private BigDecimal amount;
}
