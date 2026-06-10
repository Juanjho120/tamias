package com.tamias.maintenance.detail.controller;

import com.tamias.maintenance.detail.dto.MaintenanceRecordItemRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemResponse;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemUpdateRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonResponse;
import com.tamias.maintenance.detail.service.MaintenanceDetailService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance-records/{maintenanceRecordId}")
public class MaintenanceDetailController {

    private final MaintenanceDetailService maintenanceDetailService;

    public MaintenanceDetailController(MaintenanceDetailService maintenanceDetailService) {
        this.maintenanceDetailService = maintenanceDetailService;
    }

    @GetMapping("/people")
    public List<MaintenanceRecordPersonResponse> findPeople(@PathVariable UUID maintenanceRecordId) {
        return maintenanceDetailService.findPeople(maintenanceRecordId);
    }

    @PostMapping("/people")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecordPersonResponse addPerson(
            @PathVariable UUID maintenanceRecordId,
            @Valid @RequestBody MaintenanceRecordPersonRequest request
    ) {
        return maintenanceDetailService.addPerson(maintenanceRecordId, request);
    }

    @DeleteMapping("/people/{personAssignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePerson(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID personAssignmentId
    ) {
        maintenanceDetailService.removePerson(maintenanceRecordId, personAssignmentId);
    }

    @GetMapping({"/items", "/materials"})
    public List<MaintenanceRecordItemResponse> findItems(@PathVariable UUID maintenanceRecordId) {
        return maintenanceDetailService.findItems(maintenanceRecordId);
    }

    @PostMapping({"/items", "/materials"})
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecordItemResponse addItem(
            @PathVariable UUID maintenanceRecordId,
            @Valid @RequestBody MaintenanceRecordItemRequest request
    ) {
        return maintenanceDetailService.addItem(maintenanceRecordId, request);
    }

    @PutMapping({"/items/{itemId}", "/materials/{itemId}"})
    public MaintenanceRecordItemResponse updateItem(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MaintenanceRecordItemUpdateRequest request
    ) {
        return maintenanceDetailService.updateItem(maintenanceRecordId, itemId, request);
    }

    @DeleteMapping({"/items/{itemId}", "/materials/{itemId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID itemId
    ) {
        maintenanceDetailService.removeItem(maintenanceRecordId, itemId);
    }
}
