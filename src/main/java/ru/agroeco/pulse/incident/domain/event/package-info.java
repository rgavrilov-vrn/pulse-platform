/**
 * События жизненного цикла инцидента. Публикуются в outbox и потребляются
 * другими модулями (incidentorchestrator) через named interface "incident".
 */
@org.springframework.modulith.NamedInterface("incident")
package ru.agroeco.pulse.incident.domain.event;