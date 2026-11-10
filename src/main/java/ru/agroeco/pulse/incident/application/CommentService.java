package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.AddCommentRequest;
import ru.agroeco.pulse.incident.adapter.http.dto.CommentView;
import ru.agroeco.pulse.incident.adapter.persistence.CommentRepository;
import ru.agroeco.pulse.incident.adapter.persistence.IncidentRepository;
import ru.agroeco.pulse.incident.domain.CommentEntity;
import ru.agroeco.pulse.incident.domain.CommentType;
import ru.agroeco.pulse.incident.domain.event.IncidentCommentAdded;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;

import java.time.Instant;
import java.util.UUID;

/**
 * FIX #68: вынесено из IncidentService — устранена self-invocation
 * (внутренний вызов @Transactional-метода не перехватывается AOP).
 * Теперь IncidentService.update вызывает commentService.add(...) через
 * Spring-прокси.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String REVIEW_AUTHORITY = "incident:review";

    private final IncidentRepository incidents;
    private final CommentRepository comments;
    private final EventDispatcher events;

    @Transactional
    public CommentView add(UUID incidentId, AddCommentRequest req, UUID actorId) {
        if (!incidents.existsById(incidentId)) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }
        // FIX #69: REVIEW/RESOLUTION — только для reviewer'ов.
        if ((req.commentType() == CommentType.REVIEW
                || req.commentType() == CommentType.RESOLUTION)
                && !currentUserHasAuthority(REVIEW_AUTHORITY)) {
            throw new AccessDeniedException(
                    "Only users with " + REVIEW_AUTHORITY + " can post "
                            + req.commentType() + " comments");
        }

        CommentEntity c = CommentEntity.builder()
                .incidentId(incidentId)
                .authorId(actorId)
                .commentType(req.commentType())
                .body(req.body().trim())
                .deleted(false)
                .build();
        comments.saveAndFlush(c);

        events.dispatch(IncidentCommentAdded.builder()
                .incidentId(incidentId)
                .commentId(c.getId())
                .authorId(actorId)
                .commentType(c.getCommentType())
                .occurredAt(c.getCreatedAt() != null ? c.getCreatedAt() : Instant.now())
                .build());
        return toView(c);
    }

    private static boolean currentUserHasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private static CommentView toView(CommentEntity c) {
        return new CommentView(c.getId(), c.getIncidentId(), c.getAuthorId(),
                c.getCommentType(), c.getBody(), c.getCreatedAt());
    }
}