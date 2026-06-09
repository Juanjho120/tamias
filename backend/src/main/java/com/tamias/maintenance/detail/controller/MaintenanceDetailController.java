package com.tamias.maintenance.detail.controller;

import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedRequest;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedResponse;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedUpdateRequest;
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

    @GetMapping("/materials")
    public List<MaintenanceMaterialUsedResponse> findMaterials(@PathVariable UUID maintenanceRecordId) {
        return maintenanceDetailService.findMaterials(maintenanceRecordId);
    }

    @PostMapping("/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceMaterialUsedResponse addMaterial(
            @PathVariable UUID maintenanceRecordId,
            @Valid @RequestBody MaintenanceMaterialUsedRequest request
    ) {
        return maintenanceDetailService.addMaterial(maintenanceRecordId, request);
    }

    @PutMapping("/materials/{materialUsedId}")
    public MaintenanceMaterialUsedResponse updateMaterial(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID materialUsedId,
            @Valid @RequestBody MaintenanceMaterialUsedUpdateRequest request
    ) {
        return maintenanceDetailService.updateMaterial(maintenanceRecordId, materialUsedId, request);
    }

    @DeleteMapping("/materials/{materialUsedId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMaterial(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID materialUsedId
    ) {
        maintenanceDetailService.removeMaterial(maintenanceRecordId, materialUsedId);
    }
}
