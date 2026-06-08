package com.tamias.catalog.city.service;

import com.tamias.catalog.city.entity.City;
import com.tamias.catalog.city.repository.CityRepository;
import com.tamias.catalog.dto.CityRequest;
import com.tamias.catalog.dto.CityResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.common.dto.PageResponse;
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
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
public class CityService {

    private final CityRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public CityService(
            CityRepository repository,
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
    public PageResponse<CityResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<City> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toCityResponse));
    }

    @Transactional(readOnly = true)
    public CityResponse findById(UUID id) {
        return catalogMapper.toCityResponse(findEntity(id));
    }

    @Transactional
    public CityResponse create(CityRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("city name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        City entity = new City();
        entity.setOrganization(organization);
        catalogMapper.updateCity(entity, request);

        return catalogMapper.toCityResponse(repository.save(entity));
    }

    @Transactional
    public CityResponse update(UUID id, CityRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        City entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("city name already exists");
        }

        catalogMapper.updateCity(entity, request);

        return catalogMapper.toCityResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        City entity = findEntity(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private City findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("city not found"));
    }
}
