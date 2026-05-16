package in.codefarm.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHitDto {
    private String productId;
    private String sku;
    private String name;
    private String description;
    private String categoryId;
    private BigDecimal price;
    private Integer availableQuantity;
    private String snippet;
}
