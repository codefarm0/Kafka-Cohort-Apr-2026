package in.codefarm.order.service.repository;

import in.codefarm.order.service.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);
}