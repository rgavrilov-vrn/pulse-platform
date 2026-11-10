package ru.agroeco.pulse.trassir;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Trassir",
        // FIX #134: явно декларируем зависимость от platform (общие типы,
        // Spring-инфраструктура). Без этого любой будущий импорт из platform
        // упадёт на SpringModulith.verify().
        allowedDependencies = {"platform"}
)
public interface TrassirModule {}