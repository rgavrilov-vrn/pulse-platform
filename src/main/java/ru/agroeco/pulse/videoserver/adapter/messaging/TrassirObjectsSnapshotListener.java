package ru.agroeco.pulse.videoserver.adapter.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.agroeco.pulse.trassir.api.event.TrassirObjectsSnapshotReceived;
import ru.agroeco.pulse.videoserver.application.ReconcileVideoServersInventoryUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirObjectsSnapshotListener {

    private final ReconcileVideoServersInventoryUseCase reconcile;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TrassirObjectsSnapshotReceived event) {
        try {
            reconcile.handle(event);
        } catch (RuntimeException e) {
            log.error("Inventory reconcile failed: serverId={}, snapshotId={}",
                    event.serverId(), event.snapshotId(), e);
            // Не пробрасываем: следующий snapshot выправит состояние
        }
    }
}