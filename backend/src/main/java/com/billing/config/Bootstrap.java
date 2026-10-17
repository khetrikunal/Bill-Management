package com.billing.config;

import com.billing.domain.AppUser;
import com.billing.domain.Role;
import com.billing.repo.UserRepository;
<<<<<<< HEAD
import com.billing.service.CompanyAccessService;
=======
>>>>>>> origin/main
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Validates secrets and creates the first admin account (from env vars) when the user table is empty. */
@Component
public class Bootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProps props;
<<<<<<< HEAD
    private final CompanyAccessService companyAccess;

    public Bootstrap(UserRepository users, PasswordEncoder encoder, AppProps props, CompanyAccessService companyAccess) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
        this.companyAccess = companyAccess;
=======

    public Bootstrap(UserRepository users, PasswordEncoder encoder, AppProps props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
>>>>>>> origin/main
    }

    @Override
    public void run(String... args) {
        if (props.jwtSecret() == null || props.jwtSecret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        if (users.count() > 0) return;
        String pw = props.adminPassword();
        if (pw == null || pw.length() < 8) {
            throw new IllegalStateException("No users exist. Set ADMIN_PASSWORD (min 8 chars) to create the first admin.");
        }
        AppUser u = new AppUser();
        u.username = props.adminUsername();
        u.passwordHash = encoder.encode(pw);
        u.fullName = "Administrator";
        u.role = Role.ADMIN;
<<<<<<< HEAD
        u = users.save(u);
        companyAccess.grantAll(u.id);
=======
        users.save(u);
>>>>>>> origin/main
        log.info("Created initial admin user '{}'. Change the password after first login.", u.username);
    }
}
