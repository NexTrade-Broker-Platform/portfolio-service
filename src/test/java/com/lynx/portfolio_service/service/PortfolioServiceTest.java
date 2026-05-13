package com.lynx.portfolio_service.service;

import com.lynx.portfolio_service.portfolio.dto.request.*;
import com.lynx.portfolio_service.portfolio.dto.response.*;
import com.lynx.portfolio_service.portfolio.entity.Position;
import com.lynx.portfolio_service.portfolio.exception.InsufficientQuantityException;
import com.lynx.portfolio_service.portfolio.exception.PositionNotFoundException;
import com.lynx.portfolio_service.portfolio.repository.PositionRepository;
import com.lynx.portfolio_service.portfolio.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private UUID userId;
    private Position position;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        position = Position.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    // ─── Get Holdings Tests ───────────────────────────────────────────────────

    @Test
    void getHoldings_shouldReturnListOfHoldings() {
        when(positionRepository.findAllByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(position));

        List<HoldingResponse> holdings = portfolioService.getHoldings(userId);

        assertThat(holdings).hasSize(1);
        assertThat(holdings.get(0).getTicker()).isEqualTo("AAPL");
        assertThat(holdings.get(0).getInstrumentType()).isEqualTo("STOCK");
        assertThat(holdings.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void getHoldings_shouldReturnEmptyListWhenNoPositions() {
        when(positionRepository.findAllByUserIdAndIsActiveTrue(userId)).thenReturn(List.of());

        List<HoldingResponse> holdings = portfolioService.getHoldings(userId);

        assertThat(holdings).isEmpty();
    }

    // ─── Get Position Tests ───────────────────────────────────────────────────

    @Test
    void getPosition_shouldReturnPositionResponse() {
        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));

        PositionResponse response = portfolioService.getPosition(userId, "AAPL");

        assertThat(response).isNotNull();
        assertThat(response.getInstrumentId()).isEqualTo("AAPL");
        assertThat(response.getQuantity()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void getPosition_shouldThrowPositionNotFoundException() {
        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getPosition(userId, "AAPL"))
                .isInstanceOf(PositionNotFoundException.class);
    }

    @Test
    void getPosition_shouldThrowPositionNotFoundExceptionWhenInactive() {
        position.setActive(false);
        position.setQuantity(BigDecimal.ZERO);

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));

        assertThatThrownBy(() -> portfolioService.getPosition(userId, "AAPL"))
                .isInstanceOf(PositionNotFoundException.class);
    }

    // ─── Add Position Tests ───────────────────────────────────────────────────

    @Test
    void addPosition_shouldCreateNewPositionWhenNoneExists() {
        AddPositionRequest request = new AddPositionRequest();
        request.setUserId(userId);
        request.setInstrumentId("TSLA");
        request.setInstrumentType("STOCK");
        request.setQuantity(new BigDecimal("5.00"));
        request.setPrice(new BigDecimal("200.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "TSLA"))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        portfolioService.addPosition(request);

        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void addPosition_shouldUpdateExistingPositionAndRecalculateAverageCost() {
        AddPositionRequest request = new AddPositionRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setInstrumentType("STOCK");
        request.setQuantity(new BigDecimal("10.00"));
        request.setPrice(new BigDecimal("170.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        portfolioService.addPosition(request);

        // average cost = (150*10 + 170*10) / 20 = 160.00
        assertThat(position.getQuantity()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(position.getAverageCost()).isEqualByComparingTo(new BigDecimal("160.00"));
    }

    // ─── Reserve Quantity Tests ───────────────────────────────────────────────

    @Test
    void reserveQuantity_shouldIncreaseReservedQuantity() {
        ReserveQuantityRequest request = new ReserveQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("3.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        portfolioService.reserveQuantity(request);

        assertThat(position.getReservedQuantity()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void reserveQuantity_shouldThrowInsufficientQuantityException() {
        ReserveQuantityRequest request = new ReserveQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("999.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));

        assertThatThrownBy(() -> portfolioService.reserveQuantity(request))
                .isInstanceOf(InsufficientQuantityException.class);
    }

    // ─── Release Quantity Tests ───────────────────────────────────────────────

    @Test
    void releaseQuantity_shouldDecreaseReservedQuantity() {
        position.setReservedQuantity(new BigDecimal("5.00"));

        ReleaseQuantityRequest request = new ReleaseQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("5.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        portfolioService.releaseQuantity(request);

        assertThat(position.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void releaseQuantity_shouldThrowInsufficientQuantityException() {
        ReleaseQuantityRequest request = new ReleaseQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("999.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));

        assertThatThrownBy(() -> portfolioService.releaseQuantity(request))
                .isInstanceOf(InsufficientQuantityException.class);
    }

    // ─── Capture Quantity Tests ───────────────────────────────────────────────

    @Test
    void captureQuantity_shouldDeductQuantityAndReserved() {
        position.setReservedQuantity(new BigDecimal("5.00"));

        CaptureQuantityRequest request = new CaptureQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("5.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        portfolioService.captureQuantity(request);

        assertThat(position.getQuantity()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(position.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void captureQuantity_shouldDeactivatePositionWhenQuantityIsZero() {
        position.setQuantity(new BigDecimal("5.00"));
        position.setReservedQuantity(new BigDecimal("5.00"));

        CaptureQuantityRequest request = new CaptureQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("5.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        portfolioService.captureQuantity(request);

        assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(position.isActive()).isFalse();
    }

    @Test
    void captureQuantity_shouldThrowInsufficientQuantityException() {
        CaptureQuantityRequest request = new CaptureQuantityRequest();
        request.setUserId(userId);
        request.setInstrumentId("AAPL");
        request.setQuantity(new BigDecimal("999.00"));

        when(positionRepository.findByUserIdAndInstrumentId(userId, "AAPL"))
                .thenReturn(Optional.of(position));

        assertThatThrownBy(() -> portfolioService.captureQuantity(request))
                .isInstanceOf(InsufficientQuantityException.class);
    }
}