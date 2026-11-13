package ru.agroeco.pulse.trassir.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.trassir.application.TrassirReadService;
import ru.agroeco.pulse.trassir.application.dto.TrassirServerStateView;
import ru.agroeco.pulse.trassir.application.dto.TrassirServerView;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trassir/servers")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · Read", description = "Чтение телеметрии TRASSIR")
public class TrassirReadServerController {

    private final TrassirReadService readService;

    @GetMapping
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "List servers with last_heartbeat_at / last_snapshot_at")
    public List<TrassirServerView> list() {
        return readService.listServers();
    }

    @GetMapping("/{serverId}/state")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Get server state")
    public TrassirServerStateView state(@PathVariable UUID serverId) {
        return TrassirServerStateView.from(readService.getServer(serverId));
    }
}