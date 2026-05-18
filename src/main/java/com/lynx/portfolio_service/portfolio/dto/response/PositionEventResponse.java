package com.lynx.portfolio_service.portfolio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PositionEventResponse(
        UUID id,
        UUID userId,
        String instrumentId,
        String instrumentType,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        LocalDateTime executedAt
) {}
