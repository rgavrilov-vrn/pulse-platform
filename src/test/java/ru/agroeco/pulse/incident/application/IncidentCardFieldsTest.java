package ru.agroeco.pulse.incident.application;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import ru.agroeco.pulse.incident.adapter.http.dto.UpdateIncidentRequest;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class IncidentCardFieldsTest {
    private final NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class);
    private final IncidentCardFields fields=new IncidentCardFields(jdbc);
    private IncidentEntity incident() {
        return IncidentEntity.builder().id(UUID.randomUUID()).status(IncidentStatus.DRAFT)
                .severity(IncidentSeverity.LOW).metadata(Map.of("siteName","Старая","cameraId","camera-1")).build();
    }
    private UpdateIncidentRequest request(IncidentSeverity severity, UUID id, Boolean clear) {
        return new UpdateIncidentRequest(null,null,null,null,severity,id,clear);
    }
    @Test void severityOnlyDoesNotChangeLocationOrQueryDatabase() {
        var i=incident();var metadata=i.getMetadata();
        fields.apply(i,request(IncidentSeverity.HIGH,null,null));
        assertEquals(IncidentSeverity.HIGH,i.getSeverity());
        assertSame(metadata,i.getMetadata());verifyNoInteractions(jdbc);
    }
    @Test void facilitySynchronizesMetadataAndPreservesOtherFields() {
        var i=incident();var id=UUID.randomUUID();
        when(jdbc.queryForList(anyString(),any(SqlParameterSource.class),eq(String.class))).thenReturn(List.of("Новая"));
        when(jdbc.queryForObject(anyString(),any(SqlParameterSource.class),eq(Long.class))).thenReturn(0L);
        fields.apply(i,request(IncidentSeverity.CRITICAL,id,false));
        assertEquals(id.toString(),i.getMetadata().get("objectId"));
        assertEquals(id.toString(),i.getMetadata().get("facilityId"));
        assertEquals("Новая",i.getMetadata().get("siteName"));
        assertEquals("camera-1",i.getMetadata().get("cameraId"));
        assertEquals(IncidentSeverity.CRITICAL,i.getSeverity());
        verify(jdbc,times(2)).update(anyString(),any(SqlParameterSource.class));
    }
    @Test void clearRemovesFacilityButPreservesOtherMetadata() {
        var i=incident();
        when(jdbc.queryForObject(anyString(),any(SqlParameterSource.class),eq(Long.class))).thenReturn(0L);
        fields.apply(i,request(null,null,true));
        assertFalse(i.getMetadata().containsKey("siteName"));
        assertFalse(i.getMetadata().containsKey("facilityId"));
        assertEquals("camera-1",i.getMetadata().get("cameraId"));
        verify(jdbc).update(anyString(),any(SqlParameterSource.class));
    }
    @Test void invalidOrInactiveFacilityDoesNotWrite() {
        when(jdbc.queryForList(anyString(),any(SqlParameterSource.class),eq(String.class))).thenReturn(List.of());
        assertThrows(BusinessRuleException.class,()->fields.apply(incident(),request(null,UUID.randomUUID(),false)));
        verify(jdbc,never()).update(anyString(),any(SqlParameterSource.class));
    }
    @Test void incompatibleZoneDoesNotWrite() {
        when(jdbc.queryForList(anyString(),any(SqlParameterSource.class),eq(String.class))).thenReturn(List.of("Новая"));
        when(jdbc.queryForObject(anyString(),any(SqlParameterSource.class),eq(Long.class))).thenReturn(1L);
        assertThrows(BusinessRuleException.class,()->fields.apply(incident(),request(null,UUID.randomUUID(),false)));
        verify(jdbc,never()).update(anyString(),any(SqlParameterSource.class));
    }
    @Test void rejectsConflictingLocationCommands() {
        assertThrows(BusinessRuleException.class,()->fields.apply(incident(),request(null,UUID.randomUUID(),true)));
        verifyNoInteractions(jdbc);
    }
    @Test void publishedIncidentCannotChangeCardFields() {
        var i=incident();i.setStatus(IncidentStatus.RECORDED);
        assertThrows(BusinessRuleException.class,()->fields.apply(i,request(IncidentSeverity.HIGH,null,null)));
        verifyNoInteractions(jdbc);
    }
}
