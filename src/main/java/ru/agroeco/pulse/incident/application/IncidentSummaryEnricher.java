package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.IncidentSummary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentSummaryEnricher {
    private final NamedParameterJdbcTemplate jdbc;
    record Labels(UUID id, String siteName, String authorName, List<String> checkpointNames) {
        Labels(UUID id, String siteName, String authorName) {
            this(id,siteName,authorName,List.of());
        }
    }
    private static final String SQL="""
        SELECT i.id,
          COALESCE(NULLIF(btrim(o.name),''),NULLIF(btrim(i.metadata->>'siteName'),''),
            'Площадка не определена') AS site_name,
          COALESCE(NULLIF(btrim(u.display_name),''),NULLIF(btrim(u.login::text),''),
            i.created_by::text,'Автор не определён') AS author_name,
          ARRAY(
            SELECT DISTINCT p.name FROM (
              SELECT COALESCE(NULLIF(btrim(cp.item_name_snapshot),''),NULLIF(btrim(ci.name),'')) AS name
              FROM incident.incident_checklist_items cp
              LEFT JOIN incident.checklist_items ci ON ci.id=cp.checklist_item_id
              WHERE cp.incident_id=i.id AND NOT cp.is_deleted
              UNION ALL
              SELECT COALESCE(NULLIF(btrim(i.metadata->>'checklistItemName'),''),
                NULLIF(btrim(ci.name),''),NULLIF(btrim(i.metadata->>'checkpoint'),''))
              FROM (SELECT 1) seed LEFT JOIN LATERAL (
                SELECT c.name FROM incident.checklist_items c
                WHERE c.id::text=i.metadata->>'checklistItemId' OR c.code=i.metadata->>'checkpoint'
                ORDER BY (c.id::text=i.metadata->>'checklistItemId') DESC NULLS LAST,c.id LIMIT 1
              ) ci ON true
              WHERE NOT EXISTS(SELECT 1 FROM incident.incident_checklist_items cp
                WHERE cp.incident_id=i.id AND NOT cp.is_deleted)
            ) p WHERE p.name IS NOT NULL ORDER BY p.name
          ) AS checkpoint_names
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

    private static List<String> readCheckpointNames(ResultSet rs) throws SQLException {
        var array=rs.getArray("checkpoint_names");
        if(array==null) return List.of();
        try {
            return Arrays.stream((Object[])array.getArray()).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }

    @Transactional(readOnly=true)
    public List<IncidentSummary> enrich(List<IncidentSummary> rows) {
        if(rows.isEmpty()) return rows;
        var labels=jdbc.query(SQL,new MapSqlParameterSource("ids",rows.stream().map(IncidentSummary::id).toList()),
                (rs,n)->new Labels(rs.getObject("id",UUID.class),rs.getString("site_name"),rs.getString("author_name"),readCheckpointNames(rs)));
        var byId=new HashMap<UUID,Labels>();
        for(var label:labels) byId.put(label.id(),label);
        return rows.stream().map(row->{
            var label=byId.get(row.id());
            return new IncidentSummary(row.id(),row.number(),row.source(),row.creationMode(),
                    row.status(),row.severity(),row.kind(),row.eventTime(),row.createdAt(),
                    row.updatedAt(),row.assignedTo(),row.comment(),row.categoryId(),row.categoryName(),
                    label==null?"Площадка не определена":label.siteName(),
                    label==null?"Автор не определён":label.authorName(),
                    label==null?List.of():label.checkpointNames());
        }).toList();
    }
}
