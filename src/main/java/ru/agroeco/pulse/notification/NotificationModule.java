package ru.agroeco.pulse.notification;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Notification",
    allowedDependencies = {"platform", "auth", "refs", "audit", "outbox"}
)
public interface NotificationModule {
}
