package com.lynx.portfolio_service.portfolio.service;

import com.lynx.portfolio_service.portfolio.dto.request.*;
import com.lynx.portfolio_service.portfolio.dto.response.*;
import com.lynx.portfolio_service.portfolio.entity.Position;
import com.lynx.portfolio_service.portfolio.exception.InsufficientQuantityException;
import com.lynx.portfolio_service.portfolio.exception.PositionNotFoundException;
import com.lynx.portfolio_service.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;

    // ─── User-facing operations ───────────────────────────────────────────────

    public List<HoldingResponse> getHoldings(UUID userId) {
        return positionRepository.findAllByUserId(userId)
                .stream()
                .filter(Position::isActive)
                .map(this::toHoldingResponse)
                .collect(Collectors.toList());
    }

    public PositionResponse getPosition(UUID userId, String instrumentId) {
        Position position = findPositionOrThrow(userId, instrumentId);
        return toPositionResponse(position);
    }

    // ─── Internal operations (called by Order Service) ────────────────────────

    @Transactional
    public void addPosition(AddPositionRequest request) {
        positionRepository.findByUserIdAndInstrumentId(request.getUserId(), request.getInstrumentId())
                .ifPresentOrElse(
                        existing -> updateExistingPosition(existing, request.getQuantity(), request.getPrice()),
                        () -> createNewPosition(request)
                );
    }

    @Transactional
    public void reserveQuantity(ReserveQuantityRequest request) {
        Position position = findPositionOrThrow(request.getUserId(), request.getInstrumentId());

        BigDecimal availableQuantity = position.getQuantity().subtract(position.getReservedQuantity());
        if (availableQuantity.compareTo(request.getQuantity()) < 0) {
            throw new InsufficientQuantityException(
                    "Not enough available quantity to place this sell order.");
        }

        position.setReservedQuantity(position.getReservedQuantity().add(request.getQuantity()));
        positionRepository.save(position);
    }

    @Transactional
    public void releaseQuantity(ReleaseQuantityRequest request) {
        Position position = findPositionOrThrow(request.getUserId(), request.getInstrumentId());

        if (position.getReservedQuantity().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientQuantityException(
                    "Cannot release more than the reserved quantity.");
        }

        position.setReservedQuantity(position.getReservedQuantity().subtract(request.getQuantity()));
        positionRepository.save(position);
    }

    @Transactional
    public void captureQuantity(CaptureQuantityRequest request) {
        Position position = findPositionOrThrow(request.getUserId(), request.getInstrumentId());

        if (position.getReservedQuantity().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientQuantityException(
                    "Cannot capture more than the reserved quantity.");
        }

        position.setReservedQuantity(position.getReservedQuantity().subtract(request.getQuantity()));
        position.setQuantity(position.getQuantity().subtract(request.getQuantity()));

        if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            position.setActive(false);
        }

        positionRepository.save(position);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void updateExistingPosition(Position position, BigDecimal addedQuantity, BigDecimal price) {
        BigDecimal totalCost = position.getAverageCost().multiply(position.getQuantity())
                .add(price.multiply(addedQuantity));
        BigDecimal newQuantity = position.getQuantity().add(addedQuantity);
        BigDecimal newAverageCost = totalCost.divide(newQuantity, 4, RoundingMode.HALF_UP);

        position.setQuantity(newQuantity);
        position.setAverageCost(newAverageCost);
        position.setActive(true);
        positionRepository.save(position);
    }

    private void createNewPosition(AddPositionRequest request) {
        Position position = Position.builder()
                .userId(request.getUserId())
                .instrumentId(request.getInstrumentId())
                .instrumentType(request.getInstrumentType())
                .quantity(request.getQuantity())
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(request.getPrice())
                .build();
        positionRepository.save(position);
    }

    private Position findPositionOrThrow(UUID userId, String instrumentId) {
        return positionRepository.findByUserIdAndInstrumentId(userId, instrumentId)
                .orElseThrow(() -> new PositionNotFoundException(
                        "Position not found for user " + userId + " and instrument " + instrumentId));
    }

    private PositionResponse toPositionResponse(Position position) {
        return PositionResponse.builder()
                .id(position.getId())
                .userId(position.getUserId())
                .instrumentId(position.getInstrumentId())
                .instrumentType(position.getInstrumentType())
                .quantity(position.getQuantity())
                .reservedQuantity(position.getReservedQuantity())
                .averageCost(position.getAverageCost())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .isActive(position.isActive())
                .build();
    }

    private HoldingResponse toHoldingResponse(Position position) {
        return HoldingResponse.builder()
                .ticker(position.getInstrumentId())
                .instrumentType(position.getInstrumentType())
                .quantity(position.getQuantity())
                .averageCost(position.getAverageCost())
                .build();
    }
}