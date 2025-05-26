package ru.practicum.explorewithme.main.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateResultDto {

    @Builder.Default
    List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();

    @Builder.Default
    List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

}