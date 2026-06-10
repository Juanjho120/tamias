package com.tamias.reservation.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.reservation.dto.CancelReservationRequest;
import com.tamias.reservation.dto.ReservationRequest;
import com.tamias.reservation.dto.ReservationResponse;
import com.tamias.reservation.dto.ReservationSummaryResponse;
import com.tamias.reservation.dto.ReservationSupplyRequest;
import com.tamias.reservation.dto.ReservationSupplyResponse;
import com.tamias.reservation.dto.ReservationSupplyUpdateRequest;
import com.tamias.reservation.enums.ReservationStatus;
import com.tamias.reservation.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public PageResponse<ReservationSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ReservationStatus status,
            Pageable pageable
    ) {
        return reservationService.findAll(propertyId, status, pageable);
    }

    @GetMapping("/calendar")
    public PageResponse<ReservationSummaryResponse> findCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable
    ) {
        return reservationService.findCalendar(startDate, endDate, pageable);
    }

    @GetMapping("/{id}")
    public ReservationResponse findById(@PathVariable UUID id) {
        return reservationService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
        return reservationService.create(request);
    }

    @PutMapping("/{id}")
    public ReservationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationRequest request
    ) {
        return reservationService.update(id, request);
    }

    @GetMapping("/{id}/supplies")
    public List<ReservationSupplyResponse> findSupplies(@PathVariable UUID id) {
        return reservationService.findSupplies(id);
    }

    @PostMapping("/{id}/supplies")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationSupplyResponse addSupply(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationSupplyRequest request
    ) {
        return reservationService.addSupply(id, request);
    }

    @PutMapping("/{id}/supplies/{supplyId}")
    public ReservationSupplyResponse updateSupply(
            @PathVariable UUID id,
            @PathVariable UUID supplyId,
            @Valid @RequestBody ReservationSupplyUpdateRequest request
    ) {
        return reservationService.updateSupply(id, supplyId, request);
    }

    @DeleteMapping("/{id}/supplies/{supplyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupply(
            @PathVariable UUID id,
            @PathVariable UUID supplyId
    ) {
        reservationService.deleteSupply(id, supplyId);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationResponse cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelReservationRequest request
    ) {
        return reservationService.cancel(id, request != null ? request.reason() : null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        reservationService.delete(id);
    }
}
