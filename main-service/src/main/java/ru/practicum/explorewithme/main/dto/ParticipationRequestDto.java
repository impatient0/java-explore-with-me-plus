package ru.practicum.explorewithme.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.time.LocalDateTime;

import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMAT_PATTERN;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT_PATTERN)
    private LocalDateTime created;

    @JsonProperty("requester")
    private Long requesterId;

    @JsonProperty("event")
    private Long eventId;

    private RequestStatus status;

}
