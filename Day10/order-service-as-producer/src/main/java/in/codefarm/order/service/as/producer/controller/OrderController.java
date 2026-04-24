package in.codefarm.order.service.as.producer.controller;

import in.codefarm.order.service.as.producer.dto.OrderPlacedEvent;
import in.codefarm.order.service.as.producer.dto.OrderRequest;
import in.codefarm.order.service.as.producer.service.OrderEventProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderEventProducerService orderEventProducerService;
    
    public OrderController(OrderEventProducerService orderEventProducerService) {
        this.orderEventProducerService = orderEventProducerService;
    }

    @PostMapping("")
    public ResponseEntity<OrderPlacedEvent> placeOrderFireAndForget(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/");

            var orderPlacedEvent = orderEventProducerService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderPlacedEvent);

    }

}

