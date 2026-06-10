package com.tamias.catalog.tasktemplate.service;

import com.tamias.catalog.dto.TaskTemplateRequest;
import com.tamias.catalog.dto.TaskTemplateResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.tasktemplate.entity.TaskTemplate;
import com.tamias.catalog.tasktemplate.repository.TaskTemplateRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskTemplateService {
    private final TaskTemplateRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public TaskTemplateService(
        TaskTemplateRepository repository,
        OrganizationRepository organizationRepository,
        CurrentUserService currentUserService,
        CatalogMapper catalogMapper
    ) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.currentUserService = currentUserService;
        this.catalogMapper = catalogMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<TaskTemplateResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<TaskTemplate> page = status == null
            ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
            : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toTaskTemplateResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public TaskTemplateResponse findById(UUID id) {
        return catalogMapper.toTaskTemplateResponse(findEntity(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public TaskTemplateResponse create(TaskTemplateRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedName = request.name().trim();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("task template name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        TaskTemplate entity = new TaskTemplate();
        entity.setOrganization(organization);

        catalogMapper.updateTaskTemplate(entity, request);
        entity.setName(normalizedName);

        return catalogMapper.toTaskTemplateResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public TaskTemplateResponse update(UUID id, TaskTemplateRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        TaskTemplate entity = findEntity(id);
        String normalizedName = request.name().trim();

        if (!entity.getName().equalsIgnoreCase(normalizedName)
            && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("task template name already exists");
        }

        catalogMapper.updateTaskTemplate(entity, request);
        entity.setName(normalizedName);

        return catalogMapper.toTaskTemplateResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        TaskTemplate entity = findEntity(id);

        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());

        repository.save(entity);
    }

    private TaskTemplate findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
            .orElseThrow(() -> new NotFoundException("task template not found"));
    }

    private void validateWritableStatus(CatalogStatus status) {
        if (status == CatalogStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete task template");
        }
    }
}
