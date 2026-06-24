package com.tamias.payment.repository;

import com.tamias.payment.entity.Payment;
import com.tamias.payment.enums.PaymentMethod;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    @Query("""
            SELECT payment
            FROM Payment payment
            LEFT JOIN payment.property property
            JOIN payment.category category
            WHERE payment.organization.id = :organizationId
              AND payment.deletedAt IS NULL
              AND (:propertyId IS NULL OR property.id = :propertyId)
              AND (:categoryId IS NULL OR category.id = :categoryId)
              AND (:method IS NULL OR payment.method = :method)
              AND (:dateFrom IS NULL OR payment.payDate >= :dateFrom)
              AND (:dateTo IS NULL OR payment.payDate <= :dateTo)
              AND (
                    :searchPattern IS NULL
                    OR LOWER(payment.name) LIKE :searchPattern
                    OR LOWER(COALESCE(payment.description, '')) LIKE :searchPattern
                    OR LOWER(COALESCE(payment.responsible, '')) LIKE :searchPattern
                    OR LOWER(COALESCE(property.name, '')) LIKE :searchPattern
                    OR LOWER(COALESCE(category.name, '')) LIKE :searchPattern
              )
            """)
    Page<Payment> findAllFiltered(
            @Param("organizationId") UUID organizationId,
            @Param("propertyId") UUID propertyId,
            @Param("categoryId") UUID categoryId,
            @Param("method") PaymentMethod method,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
