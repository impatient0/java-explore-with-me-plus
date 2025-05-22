package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.explorewithme.main.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.main.model.ParticipationRequest;
import ru.practicum.explorewithme.main.model.RequestStatus;
import ru.practicum.explorewithme.main.service.params.EventRequestStatusUpdateRequestParams;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto createRequest(Long userId,Long requestEventId);

    List<ParticipationRequestDto> getRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResultDto updateRequestsStatus(EventRequestStatusUpdateRequestParams requestParams);

    List<ParticipationRequest> updateAndReturnRejectedRequests(Long eventId, RequestStatus status);
}
