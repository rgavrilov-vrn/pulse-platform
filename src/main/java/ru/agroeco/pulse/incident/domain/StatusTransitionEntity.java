package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "status_transitions", schema = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatusTransitionEntity {

    @EmbeddedId
    private StatusTransitionId id;
}