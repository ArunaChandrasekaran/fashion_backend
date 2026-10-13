package com.fashion.config;

import com.fashion.entity.AppUser;
import com.fashion.entity.Role;
import com.fashion.entity.RoleName;
import com.fashion.repository.RoleRepository;
import com.fashion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final FashionProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(FashionProperties properties,
                            UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.getAdmin().getEmail();
        String password = properties.getAdmin().getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.info("FASHION_ADMIN_EMAIL / FASHION_ADMIN_PASSWORD are not set; skipping admin bootstrap");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            log.info("Admin account already exists for {}", email.trim().toLowerCase());
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is missing from the database"));
        AppUser admin = new AppUser();
        admin.setFullName("Fashion Admin");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPhone("0000000000");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setEnabled(true);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
        log.info("Created initial ADMIN account for {}", admin.getEmail());
    }
}
