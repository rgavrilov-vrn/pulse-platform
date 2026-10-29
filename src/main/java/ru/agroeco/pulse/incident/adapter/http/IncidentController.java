package ru.agroeco.pulse.incident.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.auth.security.AppPrincipal;
import ru.agroeco.pulse.incident.adapter.http.dto.*;
import ru.agroeco.pulse.incident.application.IncidentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents")
public class IncidentController {

    private final IncidentService incidentService;

    // ------------------------------------------------------------------ CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create incident")
    public IncidentDetails create(
            @Valid @RequestBody CreateIncidentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.create(req, principal.id());
    }

    // -------------------------------------------------------------------- LIST
    @GetMapping
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "List incidents with filters and pagination")
    public PageResponse<IncidentSummary> list(
            @ModelAttribute IncidentFilter filter,
            @PageableDefault(size = 20, sort = "eventTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return incidentService.list(filter, pageable);
    }

    // ----------------------------------------------------------- CHANGE STATUS
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('incident:write', 'incident:review')")
    @Operation(summary = "Change incident status (validated against status_transitions)")
    public IncidentDetails changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.changeStatus(id, req, principal.id());
    }

    // ------------------------------------------------------------ ADD COMMENT
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add comment to incident")
    public CommentView addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.addComment(id, req, principal.id());
    }
}