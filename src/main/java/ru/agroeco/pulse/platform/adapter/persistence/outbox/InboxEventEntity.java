package ru.agroeco.pulse.platform.adapter.persistence.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events", schema = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxEventEntity {
    @EmbeddedId
    private Key id;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        @Column(name = "consumer_name", nullable = false)
        private String consumerName;
        @Column(name = "event_id", nullable = false)
        private UUID eventId;
    }
}
