package ru.agroeco.pulse.goodwan;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "GoodWan Domain",
        allowedDependencies = {"platform"}  // ← убрать "auth", "refs", "audit", "outbox" — не используются
)
public interface GoodwanModule {}