package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.IncidentSummary;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentSummaryEnricher {
    private final NamedParameterJdbcTemplate jdbc;
    record Labels(UUID id, String siteName, String authorName) {}
    private static final String SQL="""
        SELECT i.id,
          COALESCE(NULLIF(btrim(o.name),''),NULLIF(btrim(i.metadata->>'siteName'),''),
            'Площадка не определена') AS site_name,
          COALESCE(NULLIF(btrim(u.display_name),''),NULLIF(btrim(u.login::text),''),
            i.created_by::text,'Автор не определён') AS author_name
        FROM incident.incidents i
        LEFT JOIN auth.users u ON u.id=i.created_by
        LEFT JOIN ref.objects o ON o.id::text=COALESCE(
          (SELECT CASE WHEN count(DISTINCT cr.entity_id)=1 THEN min(cr.entity_id::text) END
           FROM incident.context_refs cr WHERE cr.incident_id=i.id
             AND cr.domain='ref' AND cr.entity_type IN ('object','facility')),
          NULLIF(i.metadata->>'objectId',''),NULLIF(i.metadata->>'facilityId',''),
          (SELECT CASE WHEN count(*)=1 THEN min(ro.id::text) END FROM ref.objects ro
           WHERE ro.name=i.metadata->>'siteName')
        )
        WHERE i.id IN (:ids)
        """;

    @Transactional(readOnly=true)
    public List<IncidentSummary> enrich(List<IncidentSummary> rows) {
        if(rows.isEmpty()) return rows;
        var labels=jdbc.query(SQL,new MapSqlParameterSource("ids",rows.stream().map(IncidentSummary::id).toList()),
                (rs,n)->new Labels(rs.getObject("id",UUID.class),rs.getString("site_name"),rs.getString("author_name")));
        var byId=new HashMap<UUID,Labels>();
        for(var label:labels) byId.put(label.id(),label);
        return rows.stream().map(row->{
            var label=byId.get(row.id());
            return new IncidentSummary(row.id(),row.number(),row.source(),row.creationMode(),
                    row.status(),row.severity(),row.kind(),row.eventTime(),row.createdAt(),
                    row.updatedAt(),row.assignedTo(),row.comment(),row.categoryId(),row.categoryName(),
                    label==null?"Площадка не определена":label.siteName(),
                    label==null?"Автор не определён":label.authorName());
        }).toList();
    }
}
