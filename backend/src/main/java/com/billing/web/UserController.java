package com.billing.web;

import com.billing.domain.AppUser;
import com.billing.domain.Role;
import com.billing.dto.Dtos.*;
import com.billing.repo.UserRepository;
import com.billing.service.ApiException;
<<<<<<< HEAD
import com.billing.service.CompanyAccessService;
=======
>>>>>>> origin/main
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
<<<<<<< HEAD
    private final CompanyAccessService companyAccess;

    public UserController(UserRepository users, PasswordEncoder encoder, CompanyAccessService companyAccess) {
        this.users = users;
        this.encoder = encoder;
        this.companyAccess = companyAccess;
=======

    public UserController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
>>>>>>> origin/main
    }

    @GetMapping
    public List<UserDto> list() { return users.findAll().stream().map(UserDto::of).toList(); }

    @PostMapping
<<<<<<< HEAD
    @Transactional
=======
>>>>>>> origin/main
    public UserDto create(@Valid @RequestBody UserCreate r) {
        if (users.existsByUsernameIgnoreCase(r.username())) throw ApiException.conflict("Username already exists");
        AppUser u = new AppUser();
        u.username = r.username();
        u.fullName = r.fullName().trim();
        u.role = r.role();
        u.passwordHash = encoder.encode(r.password());
<<<<<<< HEAD
        u = users.save(u);
        companyAccess.grantAll(u.id);
        return UserDto.of(u);
    }

    @GetMapping("/{id}/companies")
    public List<Long> companies(@PathVariable Long id) {
        if (!users.existsById(id)) throw ApiException.notFound("User not found");
        return companyAccess.companyIds(id);
    }

    @PutMapping("/{id}/companies")
    @Transactional
    public List<Long> setCompanies(@PathVariable Long id, @Valid @RequestBody CompanyAccessRequest request) {
        if (!users.existsById(id)) throw ApiException.notFound("User not found");
        companyAccess.replace(id, request.companyIds());
        return companyAccess.companyIds(id);
=======
        return UserDto.of(users.save(u));
>>>>>>> origin/main
    }

    @PutMapping("/{id}")
    @Transactional
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserUpdate r, Authentication a) {
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        boolean wasActiveAdmin = u.role == Role.ADMIN && u.active;
        if (r.fullName() != null && !r.fullName().isBlank()) u.fullName = r.fullName().trim();
        if (r.role() != null) u.role = r.role();
        if (r.active() != null) u.active = r.active();
        if (r.password() != null && !r.password().isBlank()) u.passwordHash = encoder.encode(r.password());
        boolean stillActiveAdmin = u.role == Role.ADMIN && u.active;
        if (wasActiveAdmin && !stillActiveAdmin && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw ApiException.conflict("At least one active admin is required.");
        }
        if (u.username.equalsIgnoreCase(a.getName()) && !u.active) {
            throw ApiException.conflict("You cannot deactivate your own account.");
        }
        return UserDto.of(users.save(u));
    }
}
