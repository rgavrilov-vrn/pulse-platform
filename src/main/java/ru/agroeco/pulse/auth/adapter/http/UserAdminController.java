package ru.agroeco.pulse.auth.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import ru.agroeco.pulse.auth.adapter.persistence.RoleRepository;
import ru.agroeco.pulse.auth.adapter.persistence.UserRepository;
import ru.agroeco.pulse.auth.domain.UserEntity;
import ru.agroeco.pulse.auth.domain.UserRoleEntity;
import ru.agroeco.pulse.auth.domain.UserRoleId;
import ru.agroeco.pulse.auth.security.AppPrincipal;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Управление пользователями")
public class UserAdminController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @PostMapping
    @PreAuthorize("hasAuthority('user:write') and hasAuthority('user:assign_role')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Create user")
    public UserView create(@Valid @RequestBody CreateUser request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password exceeds BCrypt byte limit");
        }
        if (users.existsByLogin(request.username())) {
            throw new IllegalArgumentException("User already exists");
        }

        var user = UserEntity.builder()
                .login(request.username())
                .passwordHash(encoder.encode(request.password()))
                .displayName(request.username())
                .active(true)
                .build();

        var managedUser = users.save(user);
        assignRoles(managedUser, request.roles());
        return view(managedUser);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:assign_role')")
    @Transactional
    @Operation(summary = "Assign/Change user role")
    public UserView changeRoles(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoles request,
            Authentication authentication
    ) {
        if (authentication.getPrincipal() instanceof AppPrincipal principal
                && principal.id().equals(id)) {
            throw new IllegalArgumentException("Self role changes are not allowed");
        }
        var user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        assignRoles(user, request.roles());
        return view(users.saveAndFlush(user));
    }

    private void assignRoles(UserEntity user, Set<String> roleCodes) {
        var newRoles = new HashSet<UserRoleEntity>();
        for (String roleCode : roleCodes) {
            var role = roles.findByCodeIgnoreCase(roleCode)
                    .orElseThrow(() -> new NotFoundException("Role not found: " + roleCode));
            newRoles.add(UserRoleEntity.builder()
                    .id(new UserRoleId(user.getId(), role.getId()))
                    .user(user)
                    .role(role)
                    .build());
        }
        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
    }

    private UserView view(UserEntity user) {
        var roleCodes = user.getRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .collect(java.util.stream.Collectors.toSet());
        return new UserView(user.getId(), user.getLogin(), user.isActive(), roleCodes);
    }

    public record CreateUser(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(min = 16, max = 72) String password,
            @NotEmpty Set<@NotBlank String> roles
    ) {}

    public record ChangeRoles(@NotEmpty Set<@NotBlank String> roles) {}

    public record UserView(UUID id, String username, boolean enabled, Set<String> roles) {}
}