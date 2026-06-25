package com.tamias.maintenance.detail.controller;

import com.tamias.maintenance.detail.dto.MaintenanceRecordItemRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemResponse;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemUpdateRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonResponse;
import com.tamias.maintenance.detail.dto.MaintenanceRecordServicedItemRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordServicedItemResponse;
import com.tamias.maintenance.detail.dto.MaintenanceRecordServicedItemUpdateRequest;
import com.tamias.maintenance.detail.service.MaintenanceDetailService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/serviced-items")
    public List<MaintenanceRecordServicedItemResponse> findServicedItems(@PathVariable UUID maintenanceRecordId) {
        return maintenanceDetailService.findServicedItems(maintenanceRecordId);
    }

    @PostMapping("/serviced-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecordServicedItemResponse addServicedItem(
            @PathVariable UUID maintenanceRecordId,
            @Valid @RequestBody MaintenanceRecordServicedItemRequest request
    ) {
        return maintenanceDetailService.addServicedItem(maintenanceRecordId, request);
    }

    @PutMapping("/serviced-items/{servicedItemId}")
    public MaintenanceRecordServicedItemResponse updateServicedItem(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID servicedItemId,
            @Valid @RequestBody MaintenanceRecordServicedItemUpdateRequest request
    ) {
        return maintenanceDetailService.updateServicedItem(maintenanceRecordId, servicedItemId, request);
    }

    @DeleteMapping("/serviced-items/{servicedItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeServicedItem(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID servicedItemId
    ) {
        maintenanceDetailService.removeServicedItem(maintenanceRecordId, servicedItemId);
    }
}
