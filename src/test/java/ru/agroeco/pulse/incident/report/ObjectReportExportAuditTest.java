package ru.agroeco.pulse.incident.report;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.domain.event.ObjectReportExported;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.security.AppPrincipal;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObjectReportExportAuditTest {
    private final ObjectReportController report=mock(ObjectReportController.class);
    private final EventDispatcher events=mock(EventDispatcher.class);
    private final ObjectReportExportController controller=new ObjectReportExportController(report,events,5000);
    private final AppPrincipal principal=new AppPrincipal(UUID.randomUUID(),"operator","",List.of());
    private final ObjectReportController.Filter filter=new ObjectReportController.Filter(
            null,false,null,null,null,null,null,null,null,"03","eventTime,desc");

    @Test void pdfPublishesOneAuditForWholeExport() throws IOException { success("pdf"); }
    @Test void excelPublishesOneAuditForWholeExport() throws IOException { success("xlsx"); }

    private void success(String format) throws IOException {
        var entry=mock(ObjectReportController.Entry.class);
        var first=Collections.nCopies(100,entry);
        var last=List.of(entry);
        when(report.list(filter,0,100)).thenReturn(new ObjectReportController.Page(first,0,100,101,2));
        when(report.list(filter,1,100)).thenReturn(new ObjectReportController.Page(last,1,100,101,2));
        var all=new java.util.ArrayList<>(first);
        all.addAll(last);
        byte[] bytes={1,2,3};
        try(var files=mockStatic(ObjectReportFiles.class)) {
            if("pdf".equals(format)) files.when(()->ObjectReportFiles.pdf(all,filter)).thenReturn(bytes);
            else files.when(()->ObjectReportFiles.xlsx(all,filter)).thenReturn(bytes);
            var response=controller.export(filter,format,principal);
            assertArrayEquals(bytes,response.getBody());
            assertEquals(200,response.getStatusCode().value());
        }
        var captured=ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).dispatch(captured.capture());
        verifyNoMoreInteractions(events);
        var event=assertInstanceOf(ObjectReportExported.class,captured.getValue());
        assertEquals(DomainEventType.OBJECT_REPORT_EXPORTED,event.eventType());
        assertEquals(principal.id(),event.actorId());
        assertEquals(format,event.format());
        assertEquals(filter,event.filters());
        assertEquals(101,event.rowCount());
        assertEquals(3L,event.sizeBytes());
        assertNotNull(event.occurredAt());
        assertNull(event.entityId());
        assertFalse(event.publishToOutbox());
        var snapshot=assertInstanceOf(Map.class,event.newDataSnapshot());
        assertEquals(format,snapshot.get("format"));
        assertEquals(101,snapshot.get("rowCount"));
    }

    @Test void invalidFormatDoesNotPublishAudit() {
        assertThrows(ResponseStatusException.class,()->controller.export(filter,"csv",principal));
        verifyNoInteractions(events,report);
    }

    @Test void rowLimitDoesNotPublishAudit() {
        when(report.list(filter,0,100)).thenReturn(new ObjectReportController.Page(List.of(),0,100,5001,51));
        assertThrows(ResponseStatusException.class,()->controller.export(filter,"pdf",principal));
        verifyNoInteractions(events);
    }

    @Test void generationFailureDoesNotPublishAudit() throws IOException {
        when(report.list(filter,0,100)).thenReturn(new ObjectReportController.Page(List.of(),0,100,0,0));
        try(var files=mockStatic(ObjectReportFiles.class)) {
            files.when(()->ObjectReportFiles.pdf(List.of(),filter)).thenThrow(new IOException("generation failed"));
            assertThrows(IOException.class,()->controller.export(filter,"pdf",principal));
        }
        verifyNoInteractions(events);
    }
}
