package ru.agroeco.pulse.incident.adapter.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.CommentType;

@Schema(name = "AddCommentRequest")
public record AddCommentRequest(

        @NotNull
        @Schema(description = "Тип комментария", example = "OPERATOR", requiredMode = Schema.RequiredMode.REQUIRED)
        CommentType commentType,

        @NotBlank @Size(max = 10_000)
        @Schema(description = "Текст комментария", example = "Проверил запись, нарушение подтверждено",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String body
) {}