package ru.agroeco.pulse.incident.adapter.http.report;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.application.report.ObjectReportFiles;
import ru.agroeco.pulse.incident.domain.event.ObjectReportExported;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/reports/objects/incidents")
public class ObjectReportExportController {
    private final ObjectReportController report;
    private final EventDispatcher events;
    private final int maxRows;
    public ObjectReportExportController(ObjectReportController report, EventDispatcher events,
        @Value("${app.reports.max-export-rows:5000}") int maxRows) {
        this.report=report;this.events=events;this.maxRows=Math.max(1,Math.min(maxRows,20000));
    }
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('incident:read') and hasAuthority('incident:export')")
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=IOException.class)
    public ResponseEntity<byte[]> export(@ModelAttribute ObjectReportController.Filter filter,
                                        @RequestParam String format,
                                        @AuthenticationPrincipal AppPrincipal principal) throws IOException {
        if(!"pdf".equals(format) && !"xlsx".equals(format))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Формат: pdf или xlsx");
        var first=report.list(filter,0,100);
        if(first.totalElements()>maxRows)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,"Найдено "+first.totalElements()+" записей; лимит выгрузки "+maxRows+". Уточните фильтры. Данные не обрезаны.");
        var entries=new ArrayList<>(first.content());
        for(int page=1;page<first.totalPages();page++) entries.addAll(report.list(filter,page,100).content());
        byte[] bytes="pdf".equals(format)? ObjectReportFiles.pdf(entries,filter):ObjectReportFiles.xlsx(entries,filter);
        var response=ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("pdf".equals(format)?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("object-report."+format).build().toString())
            .cacheControl(CacheControl.noStore()).body(bytes);
        events.dispatch(new ObjectReportExported(principal.id(),format,filter,entries.size(),bytes.length,Instant.now()));
        return response;
    }
}
