package ru.agroeco.pulse.auth.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.auth.adapter.persistence.RoleRepository;
import ru.agroeco.pulse.auth.adapter.persistence.UserRepository;
import ru.agroeco.pulse.auth.adapter.persistence.UserRoleRepository;
import ru.agroeco.pulse.auth.application.RoleAssignmentProperties;
import ru.agroeco.pulse.auth.domain.RoleEntity;
import ru.agroeco.pulse.auth.domain.UserEntity;
import ru.agroeco.pulse.auth.domain.UserRoleEntity;
import ru.agroeco.pulse.auth.domain.UserRoleId;
import ru.agroeco.pulse.auth.domain.event.UserCreated;
import ru.agroeco.pulse.auth.domain.event.UserDisabled;
import ru.agroeco.pulse.auth.domain.event.UserRoleChanged;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.DuplicateEventException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.platform.security.AppPrincipal;
import ru.agroeco.pulse.platform.validation.ByteLength;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Управление пользователями")
public class UserAdminController {

    private static final String ROLE_SUPERUSER = "SUPERUSER";
    private static final Set<String> ALLOWED_SORT = Set.of("login", "displayName", "createdAt");
    private static final String DEFAULT_SORT = "login";

    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final EventDispatcher events;
    private final RoleAssignmentProperties roleAssignment;

    // ------------------------------------------------------------------ LIST
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    @Transactional(readOnly = true)
    @Operation(summary = "List users (paginated)")
    public PageResponse<UserView> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable) {

        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        Page<UserEntity> page = users.search(q, enabled, safe);

        // Батч-загрузка ролей (избегаем N+1)
        Set<UUID> userIds = page.getContent().stream()
                .map(UserEntity::getId).collect(Collectors.toSet());
        Map<UUID, Set<String>> rolesByUser = userIds.isEmpty()
                ? Map.of()
                : userRoles.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        ur -> ur.getUser().getId(),
                        Collectors.mapping(ur -> ur.getRole().getCode(), Collectors.toSet())));

        return PageResponse.of(page, u -> toView(u,
                rolesByUser.getOrDefault(u.getId(), Set.of())));
    }

    // ---------------------------------------------------------------- CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('user:write') and hasAuthority('user:assign_role')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Create user")
    public UserView create(@Valid @RequestBody CreateUser request,
                           Authentication authentication) {
        UUID actorId = principalId(authentication);

        // FIX #23: business-conflict → DuplicateEventException (409), не IAE (500)
        if (users.existsByLogin(request.username())) {
            throw new DuplicateEventException("User already exists: " + request.username());
        }

        var user = UserEntity.builder()
                .login(request.username())
                .passwordHash(encoder.encode(request.password()))
                .displayName(request.displayName())
                .active(true)
                .build();
        // Сохраняем до assignRoles — чтобы @MapsId подхватил сгенерированный userId
        users.save(user);

        Set<String> assignedRoles = assignRoles(user, request.roles(), authentication, actorId);
        users.saveAndFlush(user);

        events.dispatch(UserCreated.builder()
                .userId(user.getId())
                .login(user.getLogin())
                .displayName(user.getDisplayName())
                .roles(assignedRoles)
                .actorId(actorId)
                .occurredAt(Instant.now())
                .build());

        log.info("User created: id={}, login={}, roles={}, actor={}",
                user.getId(), user.getLogin(), assignedRoles, actorId);
        return toView(user, assignedRoles);
    }

    // ---------------------------------------------------------- UPDATE (PATCH)
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    @Transactional
    @Operation(summary = "Update displayName / enabled")
    public UserView update(@PathVariable UUID id,
                           @Valid @RequestBody UpdateUser request,
                           Authentication authentication) {
        UUID actorId = principalId(authentication);

        UserEntity user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }

        boolean disabling = Boolean.FALSE.equals(request.enabled()) && user.isActive();
        if (disabling) {
            if (actorId.equals(id)) {
                throw new BusinessRuleException("Cannot disable your own account");
            }
        }

        if (request.enabled() != null) {
            user.setActive(request.enabled());
        }

        users.saveAndFlush(user);

        if (disabling) {
            events.dispatch(UserDisabled.builder()
                    .userId(id)
                    .login(user.getLogin())
                    .actorId(actorId)
                    .occurredAt(Instant.now())
                    .build());
            log.warn("User disabled: id={}, login={}, actor={}",
                    id, user.getLogin(), actorId);
        }

        return toView(user, currentRoleCodes(user));
    }

    // --------------------------------------------------------------- DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Disable user (soft delete)")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID actorId = principalId(authentication);
        if (actorId.equals(id)) {
            throw new BusinessRuleException("Cannot disable your own account");
        }
        UserEntity user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (!user.isActive()) {
            return;  // идемпотентно
        }
        user.setActive(false);
        users.saveAndFlush(user);

        events.dispatch(UserDisabled.builder()
                .userId(id)
                .login(user.getLogin())
                .actorId(actorId)
                .occurredAt(Instant.now())
                .build());
        log.warn("User disabled via DELETE: id={}, login={}, actor={}",
                id, user.getLogin(), actorId);
    }

    // -------------------------------------------------------- CHANGE ROLES
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:assign_role')")
    @Transactional
    @Operation(summary = "Assign/Change user roles")
    public UserView changeRoles(@PathVariable UUID id,
                                @Valid @RequestBody ChangeRoles request,
                                Authentication authentication) {
        UUID actorId = principalId(authentication);
        if (actorId.equals(id)) {
            throw new BusinessRuleException("Self role changes are not allowed");
        }
        UserEntity user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        Set<String> oldRoles = currentRoleCodes(user);
        Set<String> newRoles = assignRoles(user, request.roles(), authentication, actorId);
        users.saveAndFlush(user);

        if (!oldRoles.equals(newRoles)) {
            events.dispatch(UserRoleChanged.builder()
                    .userId(id)
                    .login(user.getLogin())
                    .oldRoles(oldRoles)
                    .newRoles(newRoles)
                    .actorId(actorId)
                    .occurredAt(Instant.now())
                    .build());
            log.info("User roles changed: id={}, login={}, {} -> {}, actor={}",
                    id, user.getLogin(), oldRoles, newRoles, actorId);
        }
        return toView(user, newRoles);
    }

    // ------------------------------------------------------------- helpers

    /**
     * Назначает роли с проверками:
     *  FIX #22 — защищённые роли (по умолчанию SUPERUSER, ADMIN) требуют
     *            authority {@code system:admin}; иначе — privilege escalation
     *            через user:assign_role.
     *  Bonus   — нельзя снять SUPERUSER с последнего суперюзера (lockout).
     */
    private Set<String> assignRoles(UserEntity user, Set<String> roleCodes,
                                    Authentication authentication, UUID actorId) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessRuleException("At least one role is required");
        }

        // 1. Резолвим роли
        Set<RoleEntity> resolved = new LinkedHashSet<>();
        for (String code : roleCodes) {
            RoleEntity role = roles.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new BusinessRuleException("Role not found: " + code));
            resolved.add(role);
        }
        Set<String> newRoles = resolved.stream()
                .map(RoleEntity::getCode).collect(Collectors.toCollection(LinkedHashSet::new));

        // 2. Какие роли добавляются?
        Set<String> oldRoles = currentRoleCodes(user);
        Set<String> addedRoles = new LinkedHashSet<>(newRoles);
        addedRoles.removeAll(oldRoles);

        // FIX #22: проверка защиты
        boolean isSuperAdmin = hasAuthority(authentication,
                roleAssignment.getProtectedRoleAuthority());
        for (String added : addedRoles) {
            if (roleAssignment.getProtectedRoles().contains(added) && !isSuperAdmin) {
                throw new AccessDeniedException(
                        "Cannot assign protected role '" + added
                                + "' without " + roleAssignment.getProtectedRoleAuthority());
            }
        }

        // 3. Защита от lockout последнего SUPERUSER
        if (oldRoles.contains(ROLE_SUPERUSER) && !newRoles.contains(ROLE_SUPERUSER)) {
            long remaining = userRoles.countDistinctUsersByRoleCode(ROLE_SUPERUSER);
            if (remaining <= 1) {
                throw new BusinessRuleException(
                        "Cannot remove role SUPERUSER from the last superuser");
            }
        }

        // 4. Применяем
        user.getRoles().clear();
        for (RoleEntity role : resolved) {
            user.getRoles().add(UserRoleEntity.builder()
                    .id(new UserRoleId(user.getId(), role.getId()))
                    .user(user)
                    .role(role)
                    .build());
        }
        return newRoles;
    }

    private static Set<String> currentRoleCodes(UserEntity user) {
        if (user.getRoles() == null) return Set.of();
        return user.getRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static UUID principalId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal p)) {
            throw new AccessDeniedException("No AppPrincipal in SecurityContext");
        }
        return p.id();
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private static UserView toView(UserEntity user, Set<String> roleCodes) {
        return new UserView(
                user.getId(),
                user.getLogin(),
                user.getDisplayName(),
                user.isActive(),
                roleCodes);
    }

    // --------------------------------------------------------------- DTOs

    public record CreateUser(
            @NotBlank @Size(max = 100) String username,
            // FIX #24: @Size — только на минимальную длину (chars).
            // Максимум — через @ByteLength, потому что BCrypt ограничен 72 байтами UTF-8.
            @NotBlank @Size(min = 16) @ByteLength(max = 72) String password,
            @NotBlank @Size(max = 255) String displayName,
            @NotEmpty Set<@NotBlank @Size(max = 50) String> roles
    ) {}

    public record ChangeRoles(
            @NotEmpty Set<@NotBlank @Size(max = 50) String> roles
    ) {}

    public record UpdateUser(
            @Size(max = 255) String displayName,
            Boolean enabled
    ) {}

    public record UserView(
            UUID id,
            String username,
            String displayName,
            boolean enabled,
            Set<String> roles
    ) {}
}