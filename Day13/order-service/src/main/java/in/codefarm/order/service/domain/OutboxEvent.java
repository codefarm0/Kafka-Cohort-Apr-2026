package in.codefarm.order.service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String type;
    @Column(columnDefinition = "json")
    private String payload;
    private String status; // e.g. NEW, SENT
    private Instant createdAt;
    private Instant sentAt;
    // getters/setters
}