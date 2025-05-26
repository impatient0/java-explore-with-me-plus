package ru.practicum.explorewithme.main.dto;

import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMAT_PATTERN;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.explorewithme.main.model.Location;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventAdminRequestDto {

    @Size(min = 20, max = 2000, message = "Annotation length must be between 20 and 2000 characters")
    String annotation;

    Long category;

    @Size(min = 20, max = 7000, message = "Description length must be between 20 and 7000 characters")
    String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT_PATTERN)
    @Future(message = "Event date must be in the future")
    LocalDateTime eventDate;

    Location location;

    Boolean paid;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    Integer participantLimit;

    Boolean requestModeration;

    StateActionAdmin stateAction;

    @Size(min = 3, max = 120, message = "Title length must be between 3 and 120 characters")
    String title;

    public enum StateActionAdmin {
        PUBLISH_EVENT,
        REJECT_EVENT
    }
}