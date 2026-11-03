package ru.agroeco.pulse.incident.report;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reports/objects/incidents")
@PreAuthorize("hasAuthority('incident:read')")
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
public class ObjectReportController {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public ObjectReportController(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc=jdbc; this.mapper=mapper;
    }
    public record Filter(UUID objectId, Boolean unassigned,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant eventFrom,
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant eventTo,
        String kind, List<UUID> categoryIds, List<UUID> checklistItemIds,
        List<String> statuses, UUID createdBy, String search, String sort) {}
    public record Entry(UUID id, String number, UUID objectId, String objectName,
        Instant eventTime, Instant createdAt, String kind, String category, String status,
        UUID createdBy, String operatorName, String cancellationReason, String initialComment,
        String resolutionComment, JsonNode points, JsonNode comments) {}
    public record Page(List<Entry> content, int page, int size, long totalElements, long totalPages) {}
    public record Option(UUID id, String name, UUID parentId) {}
    public record Options(List<Option> objects, List<Option> categories, List<Option> checkpoints, List<Option> operators) {}
    private record Selection(String where, MapSqlParameterSource params, String order) {}
    private static final Set<String> STATUSES=Set.of("DRAFT","RECORDED","NEEDS_REVIEW","CONFIRMED","REJECTED","CLOSED","CANCELLED","ARCHIVED");
    private static final String BASE="""
        WITH report_base AS (
          SELECT i.*, cg.code AS report_kind, vc.name AS report_category,
            COALESCE(u.display_name,u.login::text,i.created_by::text) AS report_operator,
            o.id AS report_object_id,
            COALESCE(o.name,NULLIF(i.metadata->>'siteName',''),'Объект не определён') AS report_object_name
          FROM incident.incidents i
          JOIN incident.violation_categories vc ON vc.id=i.category_id
          JOIN incident.category_groups cg ON cg.id=vc.group_id
          LEFT JOIN auth.users u ON u.id=i.created_by
          LEFT JOIN ref.objects o ON o.id::text=COALESCE(
            (SELECT CASE WHEN count(DISTINCT cr.entity_id)=1 THEN min(cr.entity_id::text) END
             FROM incident.context_refs cr WHERE cr.incident_id=i.id
             AND cr.domain='ref' AND cr.entity_type IN ('object','facility')),
            NULLIF(i.metadata->>'objectId',''), NULLIF(i.metadata->>'facilityId',''),
            (SELECT CASE WHEN count(*)=1 THEN min(ro.id::text) END FROM ref.objects ro
             WHERE ro.name=i.metadata->>'siteName')
          )
        )
        """;
    private static final String DETAILS="""
        SELECT i.*,
          COALESCE((SELECT jsonb_agg(jsonb_build_object('id',p.point_id,'name',p.point_name,'comment',p.comment) ORDER BY p.created_at,p.point_name)
            FROM (
              SELECT cp.checklist_item_id AS point_id,cp.item_name_snapshot AS point_name,cp.comment,cp.created_at
              FROM incident.incident_checklist_items cp WHERE cp.incident_id=i.id AND NOT cp.is_deleted
              UNION ALL
              SELECT ci.id,COALESCE(NULLIF(i.metadata->>'checklistItemName',''),ci.name,NULLIF(i.metadata->>'checkpoint','')),NULL::text,i.created_at
              FROM (SELECT 1) seed LEFT JOIN LATERAL (
                SELECT c.id,c.name FROM incident.checklist_items c
                WHERE c.id::text=i.metadata->>'checklistItemId' OR c.code=i.metadata->>'checkpoint'
                ORDER BY (c.id::text=i.metadata->>'checklistItemId') DESC NULLS LAST,c.id LIMIT 1
              ) ci ON true
              WHERE COALESCE(NULLIF(i.metadata->>'checklistItemName',''),ci.name,NULLIF(i.metadata->>'checkpoint','')) IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM incident.incident_checklist_items cp WHERE cp.incident_id=i.id AND NOT cp.is_deleted
                  AND (cp.checklist_item_id=ci.id OR cp.item_name_snapshot=COALESCE(NULLIF(i.metadata->>'checklistItemName',''),ci.name,NULLIF(i.metadata->>'checkpoint',''))))
            ) p),'[]'::jsonb)::text AS report_points,
          COALESCE((SELECT jsonb_agg(jsonb_build_object('source',c.source,'occurredAt',c.occurred_at,'author',c.author,'text',c.body) ORDER BY c.occurred_at,c.tie)
            FROM (
              SELECT 'COMMENT:'||cm.comment_type AS source,cm.created_at AS occurred_at,
                COALESCE(a.display_name,a.login::text,cm.author_id::text) AS author,cm.body,cm.id::text AS tie
              FROM incident.comments cm LEFT JOIN auth.users a ON a.id=cm.author_id
              WHERE cm.incident_id=i.id AND NOT cm.is_deleted
              UNION ALL
              SELECT 'STATUS:'||COALESCE(sh.from_status,'')||' → '||sh.to_status,sh.changed_at,
                COALESCE(a.display_name,a.login::text,sh.changed_by::text),sh.comment,sh.id::text
              FROM incident.status_history sh LEFT JOIN auth.users a ON a.id=sh.changed_by
              WHERE sh.incident_id=i.id AND NULLIF(btrim(sh.comment),'') IS NOT NULL
            ) c),'[]'::jsonb)::text AS report_comments
        FROM selected i
        """;
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private Selection selection(Filter f) {
        if(f.eventFrom()!=null && f.eventTo()!=null && f.eventFrom().isAfter(f.eventTo())) bad("Начало периода позже окончания");
        if(f.objectId()!=null && Boolean.TRUE.equals(f.unassigned())) bad("Выберите объект либо группу без объекта");
        if(f.kind()!=null && !Set.of("EVENT","VIOLATION").contains(f.kind())) bad("Неизвестный тип записи");
        if(f.statuses()!=null && !STATUSES.containsAll(f.statuses())) bad("Неизвестный статус");
        if(f.search()!=null && f.search().length()>500) bad("Поиск: не более 500 символов");
        if((f.categoryIds()!=null && f.categoryIds().size()>100)||(f.checklistItemIds()!=null && f.checklistItemIds().size()>100)) bad("Слишком много значений фильтра");
        var p=new MapSqlParameterSource(); var w=new StringBuilder(" WHERE true");
        if(f.objectId()!=null){w.append(" AND i.report_object_id=:objectId");p.addValue("objectId",f.objectId());}
        if(Boolean.TRUE.equals(f.unassigned()))w.append(" AND i.report_object_id IS NULL");
        if(f.eventFrom()!=null){w.append(" AND i.event_time>=:from");p.addValue("from",Timestamp.from(f.eventFrom()));}
        if(f.eventTo()!=null){w.append(" AND i.event_time<=:to");p.addValue("to",Timestamp.from(f.eventTo()));}
        if(f.kind()!=null){w.append(" AND i.report_kind=:kind");p.addValue("kind",f.kind());}
        if(f.createdBy()!=null){w.append(" AND i.created_by=:actor");p.addValue("actor",f.createdBy());}
        if(f.categoryIds()!=null && !f.categoryIds().isEmpty()){w.append(" AND i.category_id IN (:categories)");p.addValue("categories",f.categoryIds());}
        if(f.statuses()!=null && !f.statuses().isEmpty()){w.append(" AND i.status IN (:statuses)");p.addValue("statuses",f.statuses());}
        if(f.checklistItemIds()!=null && !f.checklistItemIds().isEmpty()){
            w.append(" ").append("""
                AND (EXISTS (SELECT 1 FROM incident.incident_checklist_items cp WHERE cp.incident_id=i.id AND NOT cp.is_deleted AND cp.checklist_item_id IN (:points))
                  OR EXISTS (SELECT 1 FROM incident.checklist_items ci WHERE ci.id IN (:points)
                     AND (ci.id::text=i.metadata->>'checklistItemId' OR ci.code=i.metadata->>'checkpoint')))
                """);p.addValue("points",f.checklistItemIds());
        }
        if(f.search()!=null && !f.search().isBlank()){
            p.addValue("q",f.search().trim());w.append(" ").append("""
                AND (position(lower(:q) in lower(concat_ws(' ',i.number,i.report_object_name,i.report_category,i.report_operator,i.comment,i.cancellation_reason,i.resolution_comment,i.metadata->>'checkpoint',i.metadata->>'checklistItemName')))>0
                  OR EXISTS (SELECT 1 FROM incident.comments cm WHERE cm.incident_id=i.id AND NOT cm.is_deleted AND position(lower(:q) in lower(cm.body))>0)
                  OR EXISTS (SELECT 1 FROM incident.incident_checklist_items cp WHERE cp.incident_id=i.id AND NOT cp.is_deleted AND position(lower(:q) in lower(concat_ws(' ',cp.item_name_snapshot,cp.comment)))>0)
                  OR EXISTS (SELECT 1 FROM incident.status_history sh WHERE sh.incident_id=i.id AND position(lower(:q) in lower(COALESCE(sh.comment,'')))>0))
                """);
        }
        String order=switch(f.sort()==null?"eventTime,desc":f.sort()){
            case "eventTime,desc"->"i.event_time DESC,i.id DESC";
            case "eventTime,asc"->"i.event_time ASC,i.id ASC";
            case "createdAt,desc"->"i.created_at DESC,i.id DESC";
            case "objectName,asc"->"i.report_object_name ASC,i.event_time DESC,i.id DESC";
            default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Неизвестная сортировка");
        };
        return new Selection(w.toString(),p,order);
    }
    @GetMapping
    public Page list(@ModelAttribute Filter filter,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        if(page<0 || size<1 || size>100)bad("Размер страницы 1–100; номер неотрицательный");
        var s=selection(filter);
        Long total=jdbc.queryForObject(BASE+" SELECT count(*) FROM report_base i"+s.where(),s.params(),Long.class);
        var p=s.params().addValue("limit",size).addValue("offset",(long)page*size);
        var rows=jdbc.query(BASE+", selected AS (SELECT i.* FROM report_base i"+s.where()+" ORDER BY "+s.order()+" LIMIT :limit OFFSET :offset) "+DETAILS+" ORDER BY "+s.order(),p,(rs,n)->new Entry(
            rs.getObject("id",UUID.class),rs.getString("number"),rs.getObject("report_object_id",UUID.class),rs.getString("report_object_name"),
            rs.getTimestamp("event_time").toInstant(),rs.getTimestamp("created_at").toInstant(),rs.getString("report_kind"),rs.getString("report_category"),rs.getString("status"),
            rs.getObject("created_by",UUID.class),rs.getString("report_operator"),rs.getString("cancellation_reason"),rs.getString("comment"),rs.getString("resolution_comment"),
            mapper.readTree(rs.getString("report_points")),mapper.readTree(rs.getString("report_comments"))));
        long count=total==null?0:total;
        return new Page(rows,page,size,count,(count+size-1)/size);
    }
    @GetMapping("/options")
    public Options options(){
        return new Options(options("SELECT id,name,NULL::uuid AS parent_id FROM ref.objects ORDER BY name,id"),
            options("SELECT id,name,group_id AS parent_id FROM incident.violation_categories ORDER BY sort_order,name,id"),
            options("SELECT id,name,category_id AS parent_id FROM incident.checklist_items ORDER BY sort_order,name,id"),
            options("SELECT u.id,COALESCE(u.display_name,u.login::text) AS name,NULL::uuid AS parent_id FROM auth.users u WHERE EXISTS(SELECT 1 FROM incident.incidents i WHERE i.created_by=u.id) ORDER BY name,id"));
    }
    private List<Option> options(String sql){return jdbc.query(sql,new MapSqlParameterSource(),(rs,n)->new Option(rs.getObject("id",UUID.class),rs.getString("name"),rs.getObject("parent_id",UUID.class)));}
}
