package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.IncidentSummary;
import ru.agroeco.pulse.incident.domain.IncidentKind;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IncidentSummaryEnricher {

    private final NamedParameterJdbcTemplate jdbc;

    record Labels(UUID id, String siteName, String authorName, String categoryName,
                  String groupCode, List<String> checkpointNames) {}

    /**
     * FIX: min(entity_id::text)::uuid — в PostgreSQL нет min(uuid).
     * Приведение к тексту делает UUID сортируемыми лексикографически,
     * обратное приведение — чтобы JOIN с ref.objects.id (uuid) не требовал
     * неявного каста (PostgreSQL его в ON ... = ... не делает).
     */
    private static final String SQL = """
        SELECT i.id,
          COALESCE(NULLIF(btrim(o.name), ''), 'Площадка не определена') AS site_name,
          COALESCE(NULLIF(btrim(u.display_name), ''), NULLIF(btrim(u.login::text), ''),
                   i.created_by::text, 'Автор не определён') AS author_name,
          vc.name AS category_name,
          cg.code AS group_code,
          ARRAY(
            SELECT DISTINCT p.name FROM (
              SELECT COALESCE(NULLIF(btrim(cp.item_name_snapshot), ''),
                              NULLIF(btrim(ci.name), '')) AS name
              FROM incident.incident_checklist_items cp
              LEFT JOIN incident.checklist_items ci ON ci.id = cp.checklist_item_id
              WHERE cp.incident_id = i.id AND NOT cp.is_deleted
            ) p WHERE p.name IS NOT NULL ORDER BY p.name
          ) AS checkpoint_names
        FROM incident.incidents i
        LEFT JOIN auth.users u ON u.id = i.created_by
        LEFT JOIN incident.violation_categories vc ON vc.id = i.category_id
        LEFT JOIN incident.category_groups cg ON cg.id = vc.group_id
        LEFT JOIN ref.objects o ON o.id = (
          SELECT CASE WHEN count(DISTINCT cr.entity_id) = 1
                      THEN min(cr.entity_id::text)::uuid END
          FROM incident.context_refs cr
          WHERE cr.incident_id = i.id
            AND cr.domain = 'ref'
            AND cr.entity_type IN ('object','facility')
        )
        WHERE i.id IN (:ids)
        """;

    private static List<String> readCheckpointNames(ResultSet rs) throws SQLException {
        var array = rs.getArray("checkpoint_names");
        if (array == null) return List.of();
        try {
            return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }

    @Transactional(readOnly = true)
    public List<IncidentSummary> enrich(List<IncidentSummary> rows) {
        if (rows.isEmpty()) return rows;
        var params = new MapSqlParameterSource("ids",
                rows.stream().map(IncidentSummary::id).toList());
        List<Labels> labels = jdbc.query(SQL, params,
                (rs, n) -> new Labels(
                        rs.getObject("id", UUID.class),
                        rs.getString("site_name"),
                        rs.getString("author_name"),
                        rs.getString("category_name"),
                        rs.getString("group_code"),
                        readCheckpointNames(rs)));
        Map<UUID, Labels> byId = new HashMap<>();
        for (var l : labels) byId.put(l.id(), l);

        return rows.stream().map(row -> {
            var l = byId.get(row.id());
            IncidentKind kind = (l != null)
                    ? IncidentKind.fromGroupCodeOrNull(l.groupCode())
                    : null;
            return new IncidentSummary(
                    row.id(), row.number(), row.source(), row.creationMode(),
                    row.status(), row.severity(),
                    kind,
                    row.eventTime(), row.createdAt(), row.updatedAt(),
                    row.assignedTo(), row.comment(),
                    row.categoryId(),
                    l != null ? l.categoryName() : row.categoryName(),
                    l == null ? "Площадка не определена" : l.siteName(),
                    l == null ? "Автор не определён"      : l.authorName(),
                    l == null ? List.of()                 : l.checkpointNames());
        }).toList();
    }
}