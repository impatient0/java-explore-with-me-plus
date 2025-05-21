package ru.practicum.explorewithme.main.dto;

import lombok.*;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    private Long id;

    private LocalDateTime created;

    private Long requesterId;

    private Long eventId;

    private RequestStatus status;

}
