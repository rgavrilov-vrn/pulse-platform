package ru.agroeco.pulse.incident.application;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import ru.agroeco.pulse.incident.adapter.http.dto.IncidentSummary;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IncidentSummaryEnricherTest {
    private final NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class);
    private final IncidentSummaryEnricher enricher=new IncidentSummaryEnricher(jdbc);
    private IncidentSummary row(UUID id) {
        return new IncidentSummary(id,"INC-1","manual",null,null,null,null,
                null,null,null,null,"Описание",null,"Общая");
    }
    @Test void emptyPageDoesNotQueryDatabase() {
        assertTrue(enricher.enrich(List.of()).isEmpty());
        verifyNoInteractions(jdbc);
    }
    @Test void resolvesLabelsByIdAndPreservesOrderWithOneQuery() {
        var first=row(UUID.randomUUID());
        var second=row(UUID.randomUUID());
        when(jdbc.query(anyString(),any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<IncidentSummaryEnricher.Labels>>any()))
                .thenReturn(List.of(
                        new IncidentSummaryEnricher.Labels(second.id(),"Площадка 2","operator2"),
                        new IncidentSummaryEnricher.Labels(first.id(),"Площадка 1","Иван Иванов")));
        var result=enricher.enrich(List.of(first,second));
        assertEquals(List.of(first.id(),second.id()),result.stream().map(IncidentSummary::id).toList());
        assertEquals("Площадка 1",result.getFirst().siteName());
        assertEquals("Иван Иванов",result.getFirst().authorName());
        assertEquals("operator2",result.get(1).authorName());
        assertEquals(first.comment(),result.getFirst().comment());
        assertEquals(first.categoryName(),result.getFirst().categoryName());
        verify(jdbc).query(anyString(),argThat((SqlParameterSource p)->p.getValue("ids").equals(List.of(first.id(),second.id()))),
                org.mockito.ArgumentMatchers.<RowMapper<IncidentSummaryEnricher.Labels>>any());
        verifyNoMoreInteractions(jdbc);
    }
    @Test void missingLabelsAreExplicitAndNeverUseCurrentUser() {
        when(jdbc.query(anyString(),any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<IncidentSummaryEnricher.Labels>>any())).thenReturn(List.of());
        var result=enricher.enrich(List.of(row(UUID.randomUUID()))).getFirst();
        assertEquals("Площадка не определена",result.siteName());
        assertEquals("Автор не определён",result.authorName());
    }
}
