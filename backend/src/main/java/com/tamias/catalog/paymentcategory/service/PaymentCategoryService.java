package com.tamias.catalog.paymentcategory.service;

import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.paymentcategory.entity.PaymentCategory;
import com.tamias.catalog.paymentcategory.repository.PaymentCategoryRepository;
import com.tamias.catalog.service.BaseCatalogService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentCategoryService extends BaseCatalogService<PaymentCategory> {

    public PaymentCategoryService(
            PaymentCategoryRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            CatalogMapper catalogMapper
    ) {
        super(repository, organizationRepository, currentUserService, userRepository, catalogMapper);
    }

    @Override
    protected PaymentCategory newEntity() {
        return new PaymentCategory();
    }

    @Override
    protected String getCatalogName() {
        return "payment category";
    }
}
