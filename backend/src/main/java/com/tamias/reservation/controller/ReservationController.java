package com.tamias.reservation.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.reservation.dto.CancelReservationRequest;
import com.tamias.reservation.dto.ReservationRequest;
import com.tamias.reservation.dto.ReservationResponse;
import com.tamias.reservation.dto.ReservationSummaryResponse;
import com.tamias.reservation.enums.ReservationStatus;
import com.tamias.reservation.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
