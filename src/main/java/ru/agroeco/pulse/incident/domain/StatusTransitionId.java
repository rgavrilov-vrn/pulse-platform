package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class StatusTransitionId implements Serializable {
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 32)
    private IncidentStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", length = 32)
    private IncidentStatus toStatus;
}