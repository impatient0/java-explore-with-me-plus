package ru.practicum.explorewithme.main.service;

import jakarta.validation.constraints.Positive;
import ru.practicum.explorewithme.main.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto createRequest(@Positive Long userId, @Positive Long requestEventId);

    List<ParticipationRequestDto> getRequests(@Positive Long userId);

    ParticipationRequestDto cancelRequest(@Positive Long userId, @Positive Long requestId);

}
