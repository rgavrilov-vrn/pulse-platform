package ru.agroeco.pulse.notification;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Notification",
        // Меняем "outbox" на "outbox :: outbox" для точного соответствия named interface
        allowedDependencies = {"platform", "auth", "refs", "audit", "outbox :: outbox"}
)
public interface NotificationModule {}
