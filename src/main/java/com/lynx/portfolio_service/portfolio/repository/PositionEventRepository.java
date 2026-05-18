package com.lynx.portfolio_service.portfolio.repository;

import com.lynx.portfolio_service.portfolio.entity.PositionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionEventRepository extends JpaRepository<PositionEvent, UUID> {

    List<PositionEvent> findByUserIdOrderByExecutedAtAsc(UUID userId);

    List<PositionEvent> findByUserIdAndInstrumentTypeOrderByExecutedAtAsc(UUID userId, String instrumentType);
}
