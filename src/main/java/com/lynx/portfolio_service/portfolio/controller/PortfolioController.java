package com.lynx.portfolio_service.portfolio.controller;

import com.lynx.portfolio_service.portfolio.dto.request.*;
import com.lynx.portfolio_service.portfolio.dto.response.*;
import com.lynx.portfolio_service.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void validateKey(String key){
        if (!Objects.equals(internalApiKey, key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid secret API key");
        }
    }

    // ─── User-facing endpoints (called via API Gateway) ───────────────────────

    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getHoldings(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-User-Id") UUID userId) {
        validateKey(key);
        return ResponseEntity.ok(portfolioService.getHoldings(userId));
    }

    @GetMapping("/{instrumentId}")
    public ResponseEntity<PositionResponse> getPosition(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String instrumentId) {
        validateKey(key);
        return ResponseEntity.ok(portfolioService.getPosition(userId, instrumentId));
    }

    // ─── History ─────────────────────────────────────────────────────────────

    @GetMapping("/events")
    public ResponseEntity<List<PositionEventResponse>> getPositionEvents(
            @RequestHeader("X-Internal-Key") String key,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String instrumentType) {
        validateKey(key);
        return ResponseEntity.ok(portfolioService.getPositionEvents(userId, instrumentType));
    }

    // ─── Internal endpoints (called by Order Service) ─────────────────────────

    @PostMapping("/add")
    public ResponseEntity<Void> addPosition(
            @RequestHeader("X-Internal-Key") String key,
            @Valid @RequestBody AddPositionRequest request) {
        validateKey(key);
        portfolioService.addPosition(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserveQuantity(
            @RequestHeader("X-Internal-Key") String key,
            @Valid @RequestBody ReserveQuantityRequest request) {
        validateKey(key);
        portfolioService.reserveQuantity(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseQuantity(
            @RequestHeader("X-Internal-Key") String key,
            @Valid @RequestBody ReleaseQuantityRequest request) {
        validateKey(key);
        portfolioService.releaseQuantity(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/capture")
    public ResponseEntity<Void> captureQuantity(
            @RequestHeader("X-Internal-Key") String key,
            @Valid @RequestBody CaptureQuantityRequest request) {
        validateKey(key);
        portfolioService.captureQuantity(request);
        return ResponseEntity.ok().build();
    }
}