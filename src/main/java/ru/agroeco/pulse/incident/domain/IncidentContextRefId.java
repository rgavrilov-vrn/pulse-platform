package ru.agroeco.pulse.incident.domain;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentContextRefId implements Serializable {
    private UUID incidentId;
    private String domain;
    private String entityType;
    private UUID entityId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IncidentContextRefId that)) return false;
        return Objects.equals(incidentId, that.incidentId)
                && Objects.equals(domain, that.domain)
                && Objects.equals(entityType, that.entityType)
                && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId, domain, entityType, entityId);
    }
}