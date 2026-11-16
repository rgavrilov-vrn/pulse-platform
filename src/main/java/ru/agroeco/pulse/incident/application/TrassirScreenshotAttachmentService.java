package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.application.TrassirScreenshotClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirScreenshotAttachmentService {

    private static final UUID SYSTEM_USER =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TrassirScreenshotClient screenshotClient;
    private final IncidentMaterialService materialService;

    public void attachScreenshot(
            UUID incidentId,
            String videoServerBaseUrl,
            String objectId,
            String videoTimestamp,
            String imageFile
    ) {
        try {
            if (videoServerBaseUrl == null || videoServerBaseUrl.isBlank()) {
                throw new IllegalStateException(
                        "Video server base URL is not configured: objectId=%s".formatted(objectId)
                );
            }

            byte[] image = screenshotClient.download(
                    videoServerBaseUrl,
                    objectId,
                    videoTimestamp
            );

            String fileName = imageFile != null && !imageFile.isBlank()
                    ? imageFile
                    : "trassir-%s-%s.jpg".formatted(objectId, videoTimestamp);

            materialService.uploadSystemMaterial(
                    incidentId,
                    fileName,
                    MediaType.IMAGE_JPEG_VALUE,
                    image,
                    SYSTEM_USER
            );

            log.info(
                    "Trassir screenshot attached: incidentId={}, objectId={}, file={}",
                    incidentId,
                    objectId,
                    fileName
            );
        } catch (Exception e) {
            log.warn(
                    "Failed to download or attach Trassir screenshot: incidentId={}, objectId={}, timestamp={}",
                    incidentId,
                    objectId,
                    videoTimestamp,
                    e
            );
        }
    }
}
