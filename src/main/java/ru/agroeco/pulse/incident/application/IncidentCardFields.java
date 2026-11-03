package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.UpdateIncidentRequest;
import ru.agroeco.pulse.incident.domain.IncidentEntity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class IncidentCardFields {
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(propagation=Propagation.MANDATORY)
    public void apply(IncidentEntity incident, UpdateIncidentRequest req) {
        boolean clear=Boolean.TRUE.equals(req.clearFacility());
        boolean locationChanged=req.facilityId()!=null || clear;
        if(req.severity()==null && !locationChanged) return;
        if(incident.getStatus()!=IncidentStatus.DRAFT)
            throw new BusinessRuleException("Приоритет и площадку можно менять только в черновике");
        if(clear && req.facilityId()!=null)
            throw new BusinessRuleException("Нельзя одновременно выбрать и очистить площадку");
        if(locationChanged) {
            var p=new MapSqlParameterSource("incidentId",incident.getId()).addValue("facilityId",req.facilityId());
            String name=null;
            if(!clear) {
                var names=jdbc.queryForList("SELECT name FROM ref.objects WHERE id=:facilityId AND is_active",p,String.class);
                if(names.isEmpty()) throw new BusinessRuleException("Площадка не найдена или неактивна");
                name=names.getFirst();
            }
            Long incompatible=jdbc.queryForObject("""
                SELECT count(*) FROM incident.context_refs cr
                LEFT JOIN ref.zones z ON z.id=cr.entity_id
                WHERE cr.incident_id=:incidentId AND cr.domain='ref' AND cr.entity_type='zone'
                  AND (z.id IS NULL OR z.object_id IS DISTINCT FROM CAST(:facilityId AS uuid))
                """,p,Long.class);
            if(incompatible!=null && incompatible>0)
                throw new BusinessRuleException("Нельзя изменить площадку: привязана зона другой площадки. Сначала измените контекст зоны.");
            jdbc.update("DELETE FROM incident.context_refs WHERE incident_id=:incidentId AND domain='ref' AND entity_type IN ('object','facility')",p);
            if(!clear) {
                p.addValue("name",name);
                jdbc.update("""
                    INSERT INTO incident.context_refs(incident_id,domain,entity_type,entity_id,is_primary,entity_name_snapshot)
                    SELECT :incidentId,'ref','object',:facilityId,
                      NOT EXISTS(SELECT 1 FROM incident.context_refs WHERE incident_id=:incidentId AND domain='ref' AND is_primary),:name
                    """,p);
            }
            var metadata=new HashMap<String,Object>();
            if(incident.getMetadata()!=null) metadata.putAll(incident.getMetadata());
            metadata.remove("objectId");metadata.remove("facilityId");
            metadata.remove("siteName");metadata.remove("facilityName");
            if(!clear) {
                metadata.put("objectId",req.facilityId().toString());
                metadata.put("facilityId",req.facilityId().toString());
                metadata.put("siteName",name);
            }
            incident.setMetadata(metadata);
        }
        if(req.severity()!=null) incident.setSeverity(req.severity());
    }
}
