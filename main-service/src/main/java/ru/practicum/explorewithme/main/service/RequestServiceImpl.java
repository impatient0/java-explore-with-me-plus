package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.explorewithme.main.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.RequestMapper;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.RequestRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.EventRequestStatusUpdateRequestParams;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long requestEventId) {
        ParticipationRequest result = checkRequest(userId, requestEventId);
        requestRepository.save(result);
        return requestMapper.toRequestDto(result);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest result = requestRepository.findByIdAndRequester_Id(requestId, userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User with Id = " + userId + " and Request", "Id", userId));
        result.setStatus(RequestStatus.CANCELED);
        requestRepository.save(result);
        return requestMapper.toRequestDto(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", "Id", userId));
        return requestRepository.findByRequester_Id(userId).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getCreated).reversed())
                .map(requestMapper::toRequestDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        if (!eventRepository.existsByIdAndInitiator_Id(eventId, userId))
            throw new EntityNotFoundException("Event with Id = " + eventId + " when initiator", "Id", userId);
        return requestRepository.findByEvent_Id(eventId).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getCreated).reversed())
                .map(requestMapper::toRequestDto).toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto updateRequestsStatus(EventRequestStatusUpdateRequestParams requestParams) {
        Long userId = requestParams.getUserId();
        Long eventId = requestParams.getEventId();
        List<Long> requestIdsForUpdate = requestParams.getRequestIds();
        RequestStatus statusUpdate = requestParams.getStatus();
        if (!statusUpdate.equals(RequestStatus.REJECTED) && !statusUpdate.equals(RequestStatus.CONFIRMED)) {
            throw new BusinessRuleViolationException("Only REJECTED and CONFIRMED statuses are allowed");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "Id", eventId));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new EntityNotFoundException("Event with Id = " + eventId + " when initiator", "Id", userId);
        }
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            throw new BusinessRuleViolationException("Event moderation or participant limit is not set");
        }
        if (requestRepository.countByIdInAndEvent_Id(requestIdsForUpdate, eventId) != requestIdsForUpdate.size()) {
            throw new BusinessRuleViolationException("Not all requests are for event with Id = " + eventId);
        }
        if (requestRepository
              .countByEvent_IdAndStatusEquals(eventId, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new BusinessRuleViolationException("Event participant limit reached");
        }
        LinkedHashMap<Long, ParticipationRequest> requestsMap = requestRepository.findAllByIdIn(requestIdsForUpdate).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getCreated))
                .collect(Collectors.toMap(
                        ParticipationRequest::getId,
                        request -> request,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        requestsMap.values().forEach(request -> {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new BusinessRuleViolationException("Cannot update request with status " + request.getStatus() +
                        ". Only requests with PENDING status can be updated.");
            }
        });
        EventRequestStatusUpdateResultDto result = new EventRequestStatusUpdateResultDto();
        if (statusUpdate == RequestStatus.REJECTED) {
            requestsMap.values().forEach(request -> {
                request.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(requestMapper.toRequestDto(request));
            });
            requestRepository.saveAll(requestsMap.values());
            return result;
        }

        final int[] availableRequests = {event.getParticipantLimit() -
                requestRepository.countByEvent_IdAndStatusEquals(eventId, RequestStatus.CONFIRMED)};
        requestsMap.values().forEach(request -> {
             if (availableRequests[0] > 0) {
                 request.setStatus(RequestStatus.CONFIRMED);
                 result.getConfirmedRequests().add(requestMapper.toRequestDto(request));
                 availableRequests[0]--;
             } else {
                 request.setStatus(RequestStatus.REJECTED);
                 result.getRejectedRequests().add(requestMapper.toRequestDto(request));
             }
        });
        requestRepository.saveAll(requestsMap.values());
        if (availableRequests[0] == 0) {
            List<ParticipationRequest> pendingRequests = requestRepository.findByEvent_IdAndStatus(eventId, RequestStatus.PENDING);
            if (!pendingRequests.isEmpty()) {
                pendingRequests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
                requestRepository.saveAll(pendingRequests);
                result.getRejectedRequests().addAll(pendingRequests.stream()
                        .map(requestMapper::toRequestDto).toList());
            }
        }
        return result;
    }

    private ParticipationRequest checkRequest(Long userId, Long requestEventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", "Id", userId));
        Event event = eventRepository.findById(requestEventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "Id", requestEventId));
        if (requestRepository.existsByEvent_IdAndRequester_Id(requestEventId, userId)) {
            throw new BusinessRuleViolationException("User has already requested for this event");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new BusinessRuleViolationException("User cannot participate in his own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new BusinessRuleViolationException("Event must be published");
        }
        if (event.getParticipantLimit() > 0 &&
                requestRepository.countByEvent_IdAndStatusEquals(requestEventId, RequestStatus.CONFIRMED) >=
                        event.getParticipantLimit()) {
            throw new BusinessRuleViolationException("Event participant limit reached");
        }
        ParticipationRequest newRequest = new ParticipationRequest();
        newRequest.setRequester(user);
        newRequest.setEvent(event);
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            newRequest.setStatus(RequestStatus.CONFIRMED);
        } else {
            newRequest.setStatus(RequestStatus.PENDING);
        }
        return newRequest;
    }

}
