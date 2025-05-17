package ru.practicum.explorewithme.main.dtos;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.explorewithme.main.models.EventState;
import ru.practicum.explorewithme.main.models.Location;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {

    String annotation;

    CategoryDto category;

    Long confirmedRequest;

    LocalDateTime createdOn;

    String description;

    LocalDateTime eventDate;

    Long id;

    UserShortDto initiator;

    Location location;

    Boolean paid;

    @Builder.Default
    Long participantLimit = 0L;

    LocalDateTime publishedOn;

    @Builder.Default
    Boolean requestModeration = true;

    EventState state;

    String title;

    Long views;

}
