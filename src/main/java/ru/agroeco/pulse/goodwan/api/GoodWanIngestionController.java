package ru.agroeco.pulse.goodwan.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.agroeco.pulse.goodwan.ingestion.GoodWanIngestionManager;
import ru.agroeco.pulse.goodwan.ingestion.GoodWanSchedulerState;

@RestController
@RequestMapping("/api/goodwan")
public class GoodWanIngestionController {

    private final GoodWanAuthenticationClient authenticationClient;
    private final GoodWanIngestionManager ingestionManager;

    public GoodWanIngestionController(
        GoodWanAuthenticationClient authenticationClient,
        GoodWanIngestionManager ingestionManager
    ) {
        this.authenticationClient = authenticationClient;
        this.ingestionManager = ingestionManager;
    }

    @GetMapping("/auth/status")
    public ResponseEntity<GoodWanAuthenticationStatus> getAuthenticationStatus() {
        return ResponseEntity.ok(
            authenticationClient.checkAuthentication()
        );
    }

    @PostMapping("/scheduler/start")
    public ResponseEntity<GoodWanSchedulerState> startScheduler() {
        return ResponseEntity.ok(
            ingestionManager.start()
        );
    }

    @PostMapping("/scheduler/stop")
    public ResponseEntity<GoodWanSchedulerState> stopScheduler() {
        return ResponseEntity.ok(
            ingestionManager.stop()
        );
    }

    @GetMapping("/scheduler/status")
    public ResponseEntity<GoodWanSchedulerState> getSchedulerStatus() {
        return ResponseEntity.ok(
            ingestionManager.currentState()
        );
    }
}
