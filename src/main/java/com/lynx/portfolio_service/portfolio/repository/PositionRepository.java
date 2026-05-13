package com.lynx.portfolio_service.portfolio.repository;

import com.lynx.portfolio_service.portfolio.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    List<Position> findAllByUserIdAndIsActiveTrue(UUID userId);

    Optional<Position> findByUserIdAndInstrumentId(UUID userId, String instrumentId);

    boolean existsByUserIdAndInstrumentId(UUID userId, String instrumentId);

    List<Position> findAllByUserIdAndInstrumentType(UUID userId, String instrumentType);
}