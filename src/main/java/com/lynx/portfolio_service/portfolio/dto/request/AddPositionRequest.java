package com.lynx.portfolio_service.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddPositionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Instrument ID is required")
    private String instrumentId;

    @NotBlank(message = "Instrument type is required")
    private String instrumentType;

    @NotNull(message = "Quantity is required")
    //@Positive(message = "Quantity must be strictly greater than 0")
    // I commented this and the line above bc I want to use this dto for creation and updates:
    // if the position doesn't exist we create it
    // if it exists we update the position
    // before it didn't allow selling
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be strictly greater than 0")
    private BigDecimal price;
}