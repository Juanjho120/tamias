package com.tamias.user.mapper;

import com.tamias.user.dto.UserResponse;
import com.tamias.user.dto.UserSummaryResponse;
import com.tamias.user.entity.UserOrganization;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummaryResponse(UserOrganization userOrganization) {
        var user = userOrganization.getUser();

        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                userOrganization.getRole().getCode(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    public UserResponse toResponse(UserOrganization userOrganization) {
        var user = userOrganization.getUser();

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                userOrganization.getRole().getCode(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
