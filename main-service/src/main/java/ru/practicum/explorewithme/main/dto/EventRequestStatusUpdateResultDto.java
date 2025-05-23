package ru.practicum.explorewithme.main.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResultDto {

    List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();

    List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

}