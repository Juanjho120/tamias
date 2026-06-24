package com.tamias.image.payment.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.payment.entity.PaymentImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentImageRepository extends JpaRepository<PaymentImage, UUID> {

    List<PaymentImage> findByPayment_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
            UUID paymentId,
            UUID organizationId,
            ImageStatus status
    );

    Optional<PaymentImage> findByIdAndPayment_IdAndOrganization_IdAndStatus(
            UUID id,
            UUID paymentId,
            UUID organizationId,
            ImageStatus status
    );

    List<PaymentImage> findByPayment_IdAndOrganization_Id(UUID paymentId, UUID organizationId);
}
