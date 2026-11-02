package ru.agroeco.pulse.incident.adapter.http;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class IncidentMultipartConfiguration {
    @Bean
    public MultipartConfigElement multipartConfigElement(
            @Value("${spring.servlet.multipart.max-file-size:50MB}") String fileSize,
            @Value("${spring.servlet.multipart.max-request-size:55MB}") String requestSize) {
        return new MultipartConfigElement("", DataSize.parse(fileSize).toBytes(),
                DataSize.parse(requestSize).toBytes(), 0);
    }
}
