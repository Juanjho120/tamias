package com.tamias.config;

import com.tamias.organization.entity.Organization;
import com.tamias.organization.enums.OrganizationStatus;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.RoleCode;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.RoleRepository;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDataInitializer implements CommandLineRunner {

    private static final String DEMO_ORGANIZATION_NAME = "TAMIAS Demo";
    private static final String ADMIN_EMAIL = "admin@tamias.local";
    private static final String ADMIN_PASSWORD = "Admin123!";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDataInitializer(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserOrganizationRepository userOrganizationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }

        Organization organization = new Organization();
        organization.setName(DEMO_ORGANIZATION_NAME);
        organization.setDescription("Local development organization");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization = organizationRepository.save(organization);

        User user = new User();
        user.setFirstName("Admin");
        user.setLastName("TAMIAS");
        user.setEmail(ADMIN_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        var adminRole = roleRepository.findByCode(RoleCode.ADMINISTRATOR)
                .orElseThrow(() -> new IllegalStateException("ADMINISTRATOR role not found"));

        UserOrganization userOrganization = new UserOrganization();
        userOrganization.setUser(user);
        userOrganization.setOrganization(organization);
        userOrganization.setRole(adminRole);
        userOrganization.setStatus(UserOrganizationStatus.ACTIVE);

        userOrganizationRepository.save(userOrganization);
    }
}
