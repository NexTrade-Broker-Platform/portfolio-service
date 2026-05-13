package com.lynx.portfolio_service.repository;

import com.lynx.portfolio_service.portfolio.entity.Position;
import com.lynx.portfolio_service.portfolio.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PositionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("db_portfolio_test")
            .withUsername("postgres")
            .withPassword("yourpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://dummy-issuer");
    }

    @Autowired
    private PositionRepository positionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
        userId = UUID.randomUUID();
    }

    @Test
    void save_shouldPersistPosition() {
        Position position = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();

        Position saved = positionRepository.save(position);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getInstrumentId()).isEqualTo("AAPL");
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void findByUserIdAndInstrumentId_shouldReturnPosition() {
        Position position = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();
        positionRepository.save(position);

        Optional<Position> found = positionRepository
                .findByUserIdAndInstrumentId(userId, "AAPL");

        assertThat(found).isPresent();
        assertThat(found.get().getInstrumentId()).isEqualTo("AAPL");
    }

    @Test
    void findByUserIdAndInstrumentId_shouldReturnEmptyWhenNotFound() {
        Optional<Position> found = positionRepository
                .findByUserIdAndInstrumentId(UUID.randomUUID(), "AAPL");
        assertThat(found).isEmpty();
    }

    @Test
    void findAllByUserIdAndIsActiveTrue_shouldReturnOnlyActivePositions() {
        Position stock = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();

        Position option = Position.builder()
                .userId(userId)
                .instrumentId("TSLA260417C200")
                .instrumentType("OPTION")
                .quantity(new BigDecimal("2.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("5.20"))
                .build();

        Position inactive = Position.builder()
                .userId(userId)
                .instrumentId("MSFT")
                .instrumentType("STOCK")
                .quantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("300.00"))
                .build();

        positionRepository.save(stock);
        positionRepository.save(option);
        Position savedInactive = positionRepository.save(inactive);
        savedInactive.setActive(false);
        positionRepository.save(savedInactive);

        List<Position> positions = positionRepository.findAllByUserIdAndIsActiveTrue(userId);

        assertThat(positions).hasSize(2);
        assertThat(positions).extracting(Position::getInstrumentId)
                .containsExactlyInAnyOrder("AAPL", "TSLA260417C200");
    }

    @Test
    void existsByUserIdAndInstrumentId_shouldReturnTrueWhenExists() {
        Position position = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();
        positionRepository.save(position);

        boolean exists = positionRepository
                .existsByUserIdAndInstrumentId(userId, "AAPL");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndInstrumentId_shouldReturnFalseWhenNotExists() {
        boolean exists = positionRepository
                .existsByUserIdAndInstrumentId(UUID.randomUUID(), "AAPL");
        assertThat(exists).isFalse();
    }

    @Test
    void findAllByUserIdAndInstrumentType_shouldReturnOnlyStocks() {
        Position stock = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();

        Position option = Position.builder()
                .userId(userId)
                .instrumentId("TSLA260417C200")
                .instrumentType("OPTION")
                .quantity(new BigDecimal("2.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("5.20"))
                .build();

        positionRepository.save(stock);
        positionRepository.save(option);

        List<Position> stocks = positionRepository
                .findAllByUserIdAndInstrumentType(userId, "STOCK");

        assertThat(stocks).hasSize(1);
        assertThat(stocks.get(0).getInstrumentId()).isEqualTo("AAPL");
    }

    @Test
    void save_shouldUpdateExistingPosition() {
        Position position = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();
        Position saved = positionRepository.save(position);

        saved.setQuantity(new BigDecimal("20.00"));
        Position updated = positionRepository.save(saved);

        assertThat(updated.getQuantity()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void delete_shouldRemovePosition() {
        Position position = Position.builder()
                .userId(userId)
                .instrumentId("AAPL")
                .instrumentType("STOCK")
                .quantity(new BigDecimal("10.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .averageCost(new BigDecimal("150.00"))
                .build();
        Position saved = positionRepository.save(position);

        positionRepository.delete(saved);

        Optional<Position> found = positionRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}