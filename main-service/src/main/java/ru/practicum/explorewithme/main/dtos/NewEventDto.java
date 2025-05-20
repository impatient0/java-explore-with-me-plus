package ru.practicum.explorewithme.main.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.explorewithme.main.models.Location;

import java.time.LocalDateTime;

import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMAT_PATTERN;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {

    @NotBlank(message = "Поле annotation не может быть пустым")
    @Size(min = 20, max = 2000, message = "Поле annotation должно быть от 20 до 2000 символов")
    String annotation;

    @NotNull(message = "Поле category не может быть пустым")
    Long category;

    @NotBlank(message = "Поле description не может быть пустым")
    @Size(min = 20, max = 7000, message = "Поле description должно быть от 20 до 7000 символов")
    String description;

    @NotNull(message = "Поле eventDate не может быть пустым")
    @PastOrPresent(message = "Поле eventDate должно быть не позже текущей даты и времени")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT_PATTERN)
    LocalDateTime eventDate;

    @NotNull(message = "Поле location не может быть пустым")
    Location location;

    @Builder.Default
    Boolean paid = false;

    @Builder.Default
    Long participantLimit = 0L;

    @Builder.Default
    Boolean requestModeration = true;

    @NotBlank(message = "Поле title не может быть пустым")
    @Size(min = 3, max = 120, message = "Поле title должно быть от 3 до 120 символов")
    String title;
}
