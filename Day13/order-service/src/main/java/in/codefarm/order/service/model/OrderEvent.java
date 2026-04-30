package in.codefarm.order.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
public class OrderEvent {

    private Long id;
    
    private Long userId;
    private Long courseId;
    private String status;
    private String source;
}