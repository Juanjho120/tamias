package com.tamias.payment.service;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.paymentcategory.entity.PaymentCategory;
import com.tamias.catalog.paymentcategory.repository.PaymentCategoryRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.payment.dto.PaymentRequest;
import com.tamias.payment.dto.PaymentResponse;
import com.tamias.payment.entity.Payment;
import com.tamias.payment.enums.PaymentMethod;
import com.tamias.payment.enums.PaymentStatus;
import com.tamias.payment.mapper.PaymentMapper;
import com.tamias.payment.repository.PaymentRepository;
import com.tamias.property.entity.Property;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCategoryRepository paymentCategoryRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentCategoryRepository paymentCategoryRepository,
            OrganizationRepository organizationRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            PaymentMapper paymentMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentCategoryRepository = paymentCategoryRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.paymentMapper = paymentMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<PaymentResponse> findAll(
            UUID propertyId,
            UUID categoryId,
            PaymentMethod method,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search,
            Pageable pageable
    ) {
        validateDateRange(dateFrom, dateTo);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String searchPattern = buildSearchPattern(search);

        Page<Payment> page = paymentRepository.findAllFiltered(
                organizationId,
                propertyId,
                categoryId,
                method,
                dateFrom,
                dateTo,
                searchPattern,
                pageable
        );

        return PageResponse.from(page.map(paymentMapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PaymentResponse findById(UUID id) {
        return paymentMapper.toResponse(findPayment(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PaymentResponse create(PaymentRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        User currentUser = findCurrentUser();

        Payment entity = new Payment();
        entity.setOrganization(organization);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        entity.setStatus(PaymentStatus.ACTIVE);
        paymentMapper.updateEntity(entity, request);
        setRelations(entity, request, organizationId);

        return paymentMapper.toResponse(paymentRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PaymentResponse update(UUID id, PaymentRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Payment entity = findPayment(id);
        User currentUser = findCurrentUser();

        paymentMapper.updateEntity(entity, request);
        setRelations(entity, request, organizationId);
        entity.setUpdatedBy(currentUser);

        return paymentMapper.toResponse(paymentRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        Payment entity = findPayment(id);
        User currentUser = findCurrentUser();

        entity.setStatus(PaymentStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        paymentRepository.save(entity);
    }

    private Payment findPayment(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return paymentRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    private void setRelations(Payment entity, PaymentRequest request, UUID organizationId) {
        entity.setProperty(resolveProperty(request.propertyId(), organizationId));
        entity.setCategory(resolveCategory(request.categoryId(), organizationId));
    }

    private Property resolveProperty(UUID propertyId, UUID organizationId) {
        if (propertyId == null) {
            return null;
        }

        return propertyRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(propertyId, organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));
    }

    private PaymentCategory resolveCategory(UUID categoryId, UUID organizationId) {
        PaymentCategory category = paymentCategoryRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(categoryId, organizationId)
                .orElseThrow(() -> new NotFoundException("Payment category not found"));

        if (category.getStatus() != CatalogStatus.ACTIVE) {
            throw new BadRequestException("Payment category is not active");
        }

        return category;
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be before or equal to dateTo");
        }
    }

    private String buildSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
