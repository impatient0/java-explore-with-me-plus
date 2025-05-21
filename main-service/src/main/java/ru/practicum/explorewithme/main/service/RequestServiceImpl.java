package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.RequestMapper;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.RequestRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;

import java.util.List;
import java.util.Comparator;

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
        ParticipationRequest result = requestRepository.findByIdAndRequester_Id(requestId,userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User with Id " + userId + " and Request", "Id", userId));
        result.setStatus(RequestStatus.CANCELED);
        requestRepository.save(result);
        return requestMapper.toRequestDto(result);
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> getRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", "Id", userId));
        List<ParticipationRequestDto> result = requestRepository.findByRequester_Id(userId).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getCreated).reversed())
                .map(requestMapper::toRequestDto).toList();
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

        if (event.isRequestModeration()) {
            newRequest.setStatus(RequestStatus.PENDING);
        } else {
            newRequest.setStatus(RequestStatus.CONFIRMED);
        }

        return newRequest;
    }
}
