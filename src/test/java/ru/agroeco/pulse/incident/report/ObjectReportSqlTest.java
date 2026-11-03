package ru.agroeco.pulse.incident.report;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static ru.agroeco.pulse.incident.report.ObjectReportController.*;

class ObjectReportSqlTest {
    private List<String> statements(Filter filter) {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(),any(SqlParameterSource.class),eq(Long.class))).thenReturn(0L);
        when(jdbc.query(anyString(),any(SqlParameterSource.class),org.mockito.ArgumentMatchers.<RowMapper<Entry>>any())).thenReturn(List.of());
        new ObjectReportController(jdbc,new ObjectMapper()).list(filter,0,25);
        var count=ArgumentCaptor.forClass(String.class);
        var data=ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(count.capture(),any(SqlParameterSource.class),eq(Long.class));
        verify(jdbc).query(data.capture(),any(SqlParameterSource.class),org.mockito.ArgumentMatchers.<RowMapper<Entry>>any());
        return List.of(count.getValue(),data.getValue());
    }
    private void assertSeparated(List<String> statements) {
        for(String sql:statements) {
            assertFalse(sql.contains("trueAND"),sql);
            assertFalse(sql.matches("(?s).*\\SAND\\s*\\(.*"),sql);
        }
    }
    @Test void searchOnlyHasSpaceBeforeAnd() {
        var sql=statements(new Filter(null,false,null,null,null,null,null,null,null,"03",null));
        assertSeparated(sql);
        for(String statement:sql) {
            assertTrue(statement.contains("WHERE true AND (position"),statement);
            assertTrue(statement.contains("i.number"));
        }
    }
    @Test void checkpointOnlyHasSpaceBeforeAnd() {
        var sql=statements(new Filter(null,false,null,null,null,null,List.of(UUID.randomUUID()),null,null,null,null));
        assertSeparated(sql);
        for(String statement:sql)assertTrue(statement.contains("WHERE true AND (EXISTS"),statement);
    }
    @Test void exactFiltersThenSearchAreSeparated() {
        assertSeparated(statements(new Filter(UUID.randomUUID(),false,null,null,"EVENT",List.of(UUID.randomUUID()),null,List.of("DRAFT"),UUID.randomUUID(),"03",null)));
    }
    @Test void checkpointAndSearchAreSeparated() {
        assertSeparated(statements(new Filter(null,false,null,null,null,null,List.of(UUID.randomUUID()),null,null,"03",null)));
    }
}
