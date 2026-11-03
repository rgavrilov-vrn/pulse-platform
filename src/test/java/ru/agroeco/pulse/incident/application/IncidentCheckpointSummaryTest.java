package ru.agroeco.pulse.incident.application;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import ru.agroeco.pulse.incident.adapter.http.dto.IncidentSummary;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class IncidentCheckpointSummaryTest {
    private IncidentSummary row(UUID id) {
        return new IncidentSummary(id,"INC-1","manual",null,null,null,null,
                null,null,null,null,"Описание",null,"Биобезопасность");
    }
    @Test void legacyConstructorsHaveEmptyCheckpointList() {
        assertEquals(List.of(),row(UUID.randomUUID()).checkpointNames());
    }
    @Test void mapsMultipleNamesAndReadsJdbcArray() throws Exception {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        var id=UUID.randomUUID();
        var names=List.of("Санитарные обработки","Транспортные операции");
        when(jdbc.query(anyString(),any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<IncidentSummaryEnricher.Labels>>any()))
                .thenReturn(List.of(new IncidentSummaryEnricher.Labels(id,"Площадка","operator",names)));
        var result=new IncidentSummaryEnricher(jdbc).enrich(List.of(row(id))).getFirst();
        assertEquals(names,result.checkpointNames());
        assertEquals("Биобезопасность",result.categoryName());
        assertEquals("Площадка",result.siteName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RowMapper<IncidentSummaryEnricher.Labels>> mapper=ArgumentCaptor.forClass(RowMapper.class);
        var sql=ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(),any(SqlParameterSource.class),mapper.capture());
        assertTrue(sql.getValue().contains("NOT cp.is_deleted"));
        assertTrue(sql.getValue().contains("SELECT DISTINCT p.name"));
        var rs=mock(ResultSet.class);
        var array=mock(java.sql.Array.class);
        when(rs.getObject("id",UUID.class)).thenReturn(id);
        when(rs.getArray("checkpoint_names")).thenReturn(array);
        when(array.getArray()).thenReturn(names.toArray(String[]::new));
        assertEquals(names,mapper.getValue().mapRow(rs,0).checkpointNames());
        verify(array).free();
    }
    @Test void noLabelsDoNotInventCheckpointNames() {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(),any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<IncidentSummaryEnricher.Labels>>any())).thenReturn(List.of());
        var result=new IncidentSummaryEnricher(jdbc).enrich(List.of(row(UUID.randomUUID()))).getFirst();
        assertTrue(result.checkpointNames().isEmpty());
    }
}
