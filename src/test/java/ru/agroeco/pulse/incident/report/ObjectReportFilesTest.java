package ru.agroeco.pulse.incident.report;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import tools.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.agroeco.pulse.incident.report.ObjectReportController.*;

class ObjectReportFilesTest {
    final Filter filter=new Filter(null,false,null,null,null,null,null,null,null,null,null);
    Entry entry(){var mapper=new ObjectMapper();return new Entry(UUID.randomUUID(),"INC-TEST",null,"Объект",Instant.parse("2026-01-01T00:00:00Z"),Instant.parse("2026-01-01T00:01:00Z"),"VIOLATION","Биобезопасность","CANCELLED",UUID.randomUUID(),"Оператор","Ошибочная регистрация","=1+1",null,mapper.readTree("[{\"id\":null,\"name\":\"Въезд\",\"comment\":\"Комментарий точки\"}]"),mapper.readTree("[{\"source\":\"COMMENT:OPERATOR\",\"occurredAt\":\"2026-01-01T00:02:00Z\",\"author\":\"Оператор\",\"text\":\"Уточнение\"}]"));}
    @Test void pdfContainsCyrillicAndReason()throws Exception{
        try(var doc=Loader.loadPDF(ObjectReportFiles.pdf(List.of(entry()),filter))){String text=new PDFTextStripper().getText(doc);assertTrue(text.contains("Биобезопасность"));assertTrue(text.contains("Ошибочная регистрация"));assertTrue(text.contains("Уточнение"));}
    }
    @Test void xlsxContainsTextNotFormula()throws Exception{
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(ObjectReportFiles.xlsx(List.of(entry()),filter)))){assertEquals(3,wb.getNumberOfSheets());var cell=wb.getSheet("Записи").getRow(1).getCell(9);assertEquals(CellType.STRING,cell.getCellType());assertEquals("=1+1",cell.getStringCellValue());assertTrue(wb.getSheet("Комментарии").getLastRowNum()>=4);}
    }
    @Test void emptyFilesRemainValid()throws Exception{
        try(var doc=Loader.loadPDF(ObjectReportFiles.pdf(List.of(),filter))){assertTrue(doc.getNumberOfPages()>0);}
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(ObjectReportFiles.xlsx(List.of(),filter)))){assertEquals(3,wb.getNumberOfSheets());}
    }
    @Test void rejectsOverflowWithoutSilentlyTruncating(){
        var report=mock(ObjectReportController.class);when(report.list(filter,0,100)).thenReturn(new Page(List.of(),0,100,5001,51));
        var controller=new ObjectReportExportController(report,5000);
        var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.export(filter,"pdf"));
        assertEquals(422,error.getStatusCode().value());verify(report,times(1)).list(filter,0,100);
    }
    @Test void exportsAllPages()throws Exception{
        var report=mock(ObjectReportController.class);when(report.list(filter,0,100)).thenReturn(new Page(List.of(entry()),0,100,101,2));when(report.list(filter,1,100)).thenReturn(new Page(List.of(entry()),1,100,101,2));
        var response=new ObjectReportExportController(report,5000).export(filter,"xlsx");
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))){assertEquals(2,wb.getSheet("Записи").getLastRowNum());}
        verify(report).list(filter,1,100);
    }
}
