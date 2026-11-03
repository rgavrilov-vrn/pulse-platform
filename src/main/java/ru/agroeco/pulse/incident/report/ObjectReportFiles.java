package ru.agroeco.pulse.incident.report;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tools.jackson.databind.JsonNode;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static ru.agroeco.pulse.incident.report.ObjectReportController.*;

final class ObjectReportFiles {
    private ObjectReportFiles() {}
    private static final ZoneId ZONE=ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZONE);
    private static String value(Object v){return v==null?"":v.toString();}
    private static String field(JsonNode n,String key){JsonNode v=n.get(key);return v==null||v.isNull()?"":v.asText();}
    private static String time(Instant v){return v==null?"—":TIME.format(v);}
    private static String kind(String v){return "VIOLATION".equals(v)?"Нарушение":"Событие";}
    private static String status(String v){return switch(value(v)){
        case "DRAFT"->"Черновик";case "RECORDED"->"Зарегистрировано";case "NEEDS_REVIEW"->"На рассмотрении";
        case "CONFIRMED"->"Подтверждено";case "REJECTED"->"Отклонено";case "CLOSED"->"Закрыто";
        case "CANCELLED"->"Отменено";case "ARCHIVED"->"В архиве";default->value(v);};}
    private static Instant instant(String text){return text.isBlank()?null:OffsetDateTime.parse(text).toInstant();}
    static byte[] xlsx(List<Entry> entries,Filter filter)throws IOException {
        try(var wb=new XSSFWorkbook();var output=new ByteArrayOutputStream()){
            var header=wb.createCellStyle();var font=wb.createFont();font.setBold(true);header.setFont(font);
            var wrap=wb.createCellStyle();wrap.setWrapText(true);wrap.setVerticalAlignment(VerticalAlignment.TOP);
            var dates=wb.createCellStyle();dates.setDataFormat(wb.createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));
            Sheet main=wb.createSheet("Записи"),comments=wb.createSheet("Комментарии"),params=wb.createSheet("Параметры");
            row(main,header,"Объект","Номер","Время события (МСК)","Зарегистрировано (МСК)","Тип","Категория","Контрольные точки","Статус","Оператор","Комментарий","Причина отмены","Комментарий закрытия");
            row(comments,header,"Номер","Источник","Время (МСК)","Автор","Текст","Часть");
            for(var e:entries){
                var names=new ArrayList<String>();for(var p:e.points())names.add(field(p,"name"));
                Row r=row(main,wrap,e.objectName(),e.number(),"","",kind(e.kind()),e.category(),String.join("; ",names),status(e.status()),e.operatorName(),e.initialComment(),e.cancellationReason(),e.resolutionComment());
                date(r.getCell(2),e.eventTime(),dates);date(r.getCell(3),e.createdAt(),dates);
                comment(comments,wrap,dates,e.number(),"Исходный комментарий",e.createdAt(),e.operatorName(),e.initialComment());
                for(var p:e.points())comment(comments,wrap,dates,e.number(),"Контрольная точка: "+field(p,"name"),null,"",field(p,"comment").isBlank()?"Без комментария":field(p,"comment"));
                for(var c:e.comments())comment(comments,wrap,dates,e.number(),field(c,"source"),instant(field(c,"occurredAt")),field(c,"author"),field(c,"text"));
                comment(comments,wrap,dates,e.number(),"Причина отмены",null,"",e.cancellationReason());
                comment(comments,wrap,dates,e.number(),"Комментарий закрытия",null,"",e.resolutionComment());
            }
            row(params,header,"Параметр","Значение");row(params,wrap,"Сформировано (МСК)",time(Instant.now()));
            row(params,wrap,"Всего записей",entries.size());row(params,wrap,"Фильтры",filter);
            row(params,wrap,"Примечание","Полные комментарии и контрольные точки — на листе Комментарии. Даты — МСК.");
            for(Sheet sh:List.of(main,comments,params)){
                sh.createFreezePane(0,1);int cols=sh.getRow(0).getLastCellNum();
                sh.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,sh.getLastRowNum(),0,cols-1));
                for(int n=0;n<cols;n++)sh.setColumnWidth(n,((sh==comments&&n==4)||(sh==params&&n==1)?70:25)*256);
            }
            wb.write(output);return output.toByteArray();
        }
    }
    private static Row row(Sheet sh,CellStyle style,Object...values){
        Row r=sh.createRow(sh.getPhysicalNumberOfRows()==0?0:sh.getLastRowNum()+1);
        for(int n=0;n<values.length;n++){
            Cell c=r.createCell(n);String s=value(values[n]);
            c.setCellValue(s.length()>32767?s.substring(0,32600)+" … Полные данные — на листе Комментарии":s);c.setCellStyle(style);
        }return r;
    }
    private static void date(Cell cell,Instant value,CellStyle style){if(value!=null){cell.setCellValue(LocalDateTime.ofInstant(value,ZONE));cell.setCellStyle(style);}}
    private static void comment(Sheet sh,CellStyle wrap,CellStyle dates,String number,String source,Instant at,String author,String text){
        if(text==null||text.isBlank())return;
        int part=1;
        for(int p=0;p<text.length();){
            int end=Math.min(text.length(),p+32000);if(end<text.length()&&Character.isHighSurrogate(text.charAt(end-1)))end--;
            Row r=row(sh,wrap,number,source,"",author,text.substring(p,end),part++);date(r.getCell(2),at,dates);p=end;
        }
    }
    static byte[] pdf(List<Entry> entries,Filter filter)throws IOException {
        try(var doc=new PDDocument();var input=PDDocument.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");var output=new ByteArrayOutputStream()){
            if(input==null)throw new IOException("PDF font resource is missing");
            var font=PDType0Font.load(doc,input,true);
            try(var writer=new PdfWriter(doc,font)){
                writer.line("Отчёт по объектам",16);writer.line("Сформировано: "+time(Instant.now())+" МСК",10);
                writer.line("Всего записей: "+entries.size(),10);writer.line("Фильтры: "+filter,9);
                Map<String,List<Entry>> groups=new LinkedHashMap<>();for(var e:entries)groups.computeIfAbsent(e.objectName(),k->new ArrayList<>()).add(e);
                for(var group:groups.entrySet()){
                    writer.line("Объект: "+group.getKey(),14);
                    for(var e:group.getValue()){
                        writer.line(e.number()+" · "+kind(e.kind())+" · "+status(e.status()),12);
                        writer.line("Событие: "+time(e.eventTime())+" МСК; регистрация: "+time(e.createdAt())+" МСК",10);
                        writer.line("Оператор: "+value(e.operatorName())+"; категория: "+value(e.category()),10);
                        if(e.points().isEmpty())writer.line("Контрольная точка: не указана",10);
                        for(var p:e.points())writer.line("Контрольная точка: "+field(p,"name")+(field(p,"comment").isBlank()?"":". "+field(p,"comment")),10);
                        writer.line("Исходный комментарий: "+(value(e.initialComment()).isBlank()?"не указан":e.initialComment()),10);
                        for(var c:e.comments())writer.line(time(instant(field(c,"occurredAt")))+" · "+field(c,"author")+" · "+field(c,"source")+": "+field(c,"text"),9);
                        if(!value(e.cancellationReason()).isBlank()||"CANCELLED".equals(e.status()))writer.line("Причина отмены: "+(value(e.cancellationReason()).isBlank()?"не указана":e.cancellationReason()),10);
                        if(!value(e.resolutionComment()).isBlank())writer.line("Комментарий закрытия: "+e.resolutionComment(),10);
                        writer.line(" ",8);
                    }
                }
            }
            int count=doc.getNumberOfPages();
            for(int n=0;n<count;n++)try(var cs=new PDPageContentStream(doc,doc.getPage(n),PDPageContentStream.AppendMode.APPEND,true,true)){
                cs.beginText();cs.setFont(font,9);cs.newLineAtOffset(40,25);cs.showText("Страница "+(n+1)+" из "+count);cs.endText();
            }
            doc.save(output);return output.toByteArray();
        }
    }
    private static final class PdfWriter implements AutoCloseable {
        private final PDDocument doc;private final PDType0Font font;private PDPageContentStream cs;private float y;
        PdfWriter(PDDocument doc,PDType0Font font){this.doc=doc;this.font=font;}
        private void page()throws IOException{if(cs!=null)cs.close();var p=new PDPage(PDRectangle.A4);doc.addPage(p);cs=new PDPageContentStream(doc,p);y=800;}
        void line(String text,float size)throws IOException{
            StringBuilder safe=new StringBuilder();
            for(int cp:text.replace('\t',' ').codePoints().toArray()){
                if(cp=='\n'){safe.append('\n');continue;}if(Character.isISOControl(cp))continue;
                String ch=new String(Character.toChars(cp));try{font.encode(ch);safe.append(ch);}catch(IllegalArgumentException ex){safe.append('?');}
            }
            for(String paragraph:safe.toString().split("\n",-1)){
                StringBuilder line=new StringBuilder();
                for(int cp:paragraph.codePoints().toArray()){
                    String ch=new String(Character.toChars(cp));
                    if(line.length()>0&&font.getStringWidth(line.toString()+ch)*size/1000>510){
                        int space=line.lastIndexOf(" ");
                        if(space>0){draw(line.substring(0,space),size);String tail=line.substring(space+1);line.setLength(0);line.append(tail);}
                        else{draw(line.toString(),size);line.setLength(0);}
                    }
                    line.append(ch);
                }draw(line.toString(),size);
            }
        }
        private void draw(String text,float size)throws IOException{if(cs==null||y<size+50)page();cs.beginText();cs.setFont(font,size);cs.newLineAtOffset(40,y);cs.showText(text);cs.endText();y-=size*1.5f;}
        @Override public void close()throws IOException{if(cs!=null)cs.close();}
    }
}
