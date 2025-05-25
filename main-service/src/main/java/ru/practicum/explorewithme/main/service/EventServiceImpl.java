package ru.practicum.explorewithme.main.service;

import com.querydsl.core.BooleanBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.dto.NewEventDto;
import ru.practicum.explorewithme.main.dto.UpdateEventAdminRequestDto;
import ru.practicum.explorewithme.main.dto.UpdateEventUserRequestDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.error.ValidationException;
import ru.practicum.explorewithme.main.mapper.EventMapper;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.RequestRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;
import ru.practicum.explorewithme.main.service.params.PublicEventSearchParams;
import ru.practicum.explorewithme.stats.server.service.StatsService;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsService statsService;

    private static final long MIN_HOURS_BEFORE_PUBLICATION_FOR_ADMIN = 1;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsPublic(PublicEventSearchParams params, int from, int size, String ipAddress) {
        log.debug("Public search for events with params: {}", params);

        String text = params.getText();
        List<Long> categories = params.getCategories();
        Boolean paid = params.getPaid();
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        boolean onlyAvailable = params.isOnlyAvailable();
        String sort = params.getSort();

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart cannot be after rangeEnd.");
        }

        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // Только опубликованные события
        predicate.and(qEvent.state.eq(EventState.PUBLISHED));

        if (text != null && !text.isBlank()) {
            String searchText = text.toLowerCase();
            predicate.and(qEvent.annotation.lower().contains(searchText)
                    .or(qEvent.description.lower().contains(searchText)));
        }

        if (categories != null && !categories.isEmpty()) {
            predicate.and(qEvent.category.id.in(categories));
        }

        if (paid != null) {
            predicate.and(qEvent.paid.eq(paid));
        }

        if (rangeStart != null) {
            predicate.and(qEvent.eventDate.goe(rangeStart));
        } else {
            predicate.and(qEvent.eventDate.goe(LocalDateTime.now()));
        }

        if (rangeEnd != null) {
            predicate.and(qEvent.eventDate.loe(rangeEnd));
        }

        if (onlyAvailable) {
            predicate.and(qEvent.participantLimit.eq(0));
        }

        Sort sortOption = sort != null && sort.equals("VIEWS") ?
                Sort.by(Sort.Direction.DESC, "views") :
                Sort.by(Sort.Direction.ASC, "eventDate");

        Pageable pageable = PageRequest.of(from / size, size, sortOption);

        Page<Event> eventPage = eventRepository.findAll(predicate, pageable);

        if (eventPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> result = eventMapper.toEventShortDtoList(eventPage.getContent());
        log.debug("Public search found {} events", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventByIdPublic(Long eventId, String ipAddress) {
        log.debug("Public: Fetching event id={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " not found."));

        if (event.getState() != EventState.PUBLISHED) {
            throw new EntityNotFoundException("Event with id=" + eventId + " is not published.");
        }

        if (ipAddress != null && !ipAddress.isBlank()) {
            statsService.incrementView(eventId, ipAddress);
        }
        EventFullDto result = eventMapper.toEventFullDto(event);
        result.setViews(statsService.getViewsForEvent(eventId));
        log.debug("Public: Found event: {}", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEventsAdmin(AdminEventSearchParams params, int from, int size) {
        List<Long> users = params.getUsers();
        List<EventState> states = params.getStates();
        List<Long> categories = params.getCategories();
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        log.debug("Admin search for events with params: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            log.warn("Admin search: rangeStart cannot be after rangeEnd. rangeStart={}, rangeEnd={}", rangeStart, rangeEnd);
            throw new IllegalArgumentException("Admin search: rangeStart cannot be after rangeEnd.");
        }

        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        if (users != null && !users.isEmpty()) {
            predicate.and(qEvent.initiator.id.in(users));
        }

        if (states != null && !states.isEmpty()) {
            predicate.and(qEvent.state.in(states));
        }

        if (categories != null && !categories.isEmpty()) {
            predicate.and(qEvent.category.id.in(categories));
        }

        if (rangeStart != null) {
            predicate.and(qEvent.eventDate.goe(rangeStart));
        }

        if (rangeEnd != null) {
            predicate.and(qEvent.eventDate.loe(rangeEnd));
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Event> eventPage = eventRepository.findAll(predicate, pageable);

        if (eventPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventFullDto> result = eventPage.getContent().stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toEventFullDto(event);
                    Long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
                    dto.setConfirmedRequests(confirmedRequests);
                    return dto;
                })
                .collect(Collectors.toList());
        log.debug("Admin search found {} events on page {}/{}", result.size(), pageable.getPageNumber(), eventPage.getTotalPages());
        return result;
    }

    @Override
    public EventFullDto moderateEventByAdmin(Long eventId, UpdateEventAdminRequestDto requestDto) {
        log.info("Admin: Moderating event id={} with data: {}", eventId, requestDto);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " not found."));

        if (requestDto.getEventDate() != null && requestDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least two hours in the future");
        }

        if (requestDto.getAnnotation() != null) {
            event.setAnnotation(requestDto.getAnnotation());
        }
        if (requestDto.getCategory() != null) {
            Category category = categoryRepository.findById(requestDto.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category with id=" + requestDto.getCategory() + " not found for event update."));
            event.setCategory(category);
        }
        if (requestDto.getDescription() != null) {
            event.setDescription(requestDto.getDescription());
        }
        if (requestDto.getEventDate() != null) {
            event.setEventDate(requestDto.getEventDate());
        }
        if (requestDto.getLocation() != null) {
            event.setLocation(requestDto.getLocation());
        }
        if (requestDto.getPaid() != null) {
            event.setPaid(requestDto.getPaid());
        }
        if (requestDto.getParticipantLimit() != null) {
            event.setParticipantLimit(requestDto.getParticipantLimit());
        }
        if (requestDto.getRequestModeration() != null) {
            event.setRequestModeration(requestDto.getRequestModeration());
        }
        if (requestDto.getTitle() != null) {
            event.setTitle(requestDto.getTitle());
        }

        if (requestDto.getStateAction() != null) {
            switch (requestDto.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new BusinessRuleViolationException(
                                "Cannot publish the event because it's not in the PENDING state. Current state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLICATION_FOR_ADMIN))) {
                        throw new BusinessRuleViolationException(
                                String.format("Cannot publish the event. Event date must be at least %d hour(s) in the future from the current moment. Event date: %s",
                                        MIN_HOURS_BEFORE_PUBLICATION_FOR_ADMIN, event.getEventDate()));
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new BusinessRuleViolationException(
                                "Cannot reject the event because it has already been published. Current state: " + event.getState());
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    log.warn("Admin: Unknown state action for event update: {}", requestDto.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Admin: Event id={} moderated successfully. New state: {}", eventId, updatedEvent.getState());
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByOwner(Long userId, int from, int size) {
        log.debug("Fetching events for owner (user) id: {}, from: {}, size: {}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "eventDate"));

        Page<Event> eventPage = eventRepository.findByInitiatorId(userId, pageable);

        if (eventPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> result = eventMapper.toEventShortDtoList(eventPage.getContent());
        log.debug("Found {} events for owner id: {} on page {}/{}", result.size(), userId, pageable.getPageNumber(), eventPage.getTotalPages());
        return result;
    }

    @Override
    public EventFullDto updateEventByOwner(Long userId, Long eventId, UpdateEventUserRequestDto requestDto) {
        log.info("User id={}: Updating event id={} with data: {}", userId, eventId, requestDto);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Event with id=%d and initiatorId=%d not found", eventId, userId)));

        if (!(event.getState() == EventState.PENDING || event.getState() == EventState.CANCELED)) {
            throw new BusinessRuleViolationException("Cannot update event: Only pending or canceled events can be changed. Current state: " + event.getState());
        }

        if (requestDto.getEventDate() != null && requestDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least two hours in the future");
        }

        if (requestDto.getAnnotation() != null) {
            event.setAnnotation(requestDto.getAnnotation());
        }
        if (requestDto.getCategory() != null) {
            Category category = categoryRepository.findById(requestDto.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category with id=" + requestDto.getCategory() + " not found."));
            event.setCategory(category);
        }
        if (requestDto.getDescription() != null) {
            event.setDescription(requestDto.getDescription());
        }
        if (requestDto.getEventDate() != null) {
            event.setEventDate(requestDto.getEventDate());
        }
        if (requestDto.getLocation() != null) {
            event.setLocation(requestDto.getLocation());
        }
        if (requestDto.getPaid() != null) {
            event.setPaid(requestDto.getPaid());
        }
        if (requestDto.getParticipantLimit() != null) {
            event.setParticipantLimit(requestDto.getParticipantLimit());
        }
        if (requestDto.getRequestModeration() != null) {
            event.setRequestModeration(requestDto.getRequestModeration());
        }
        if (requestDto.getTitle() != null) {
            event.setTitle(requestDto.getTitle());
        }

        if (requestDto.getStateAction() != null) {
            switch (requestDto.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    log.warn("Unknown state action for user update: {}", requestDto.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("User id={}: Event id={} updated successfully.", userId, eventId);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventPrivate(Long userId, Long eventId) {
        log.debug("Fetching event id: {} for user id: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Event with id=%d and initiatorId=%d not found", eventId, userId)));

        EventFullDto result = eventMapper.toEventFullDto(event);
        log.debug("Found event: {}", result);
        return result;
    }

    @Override
    public EventFullDto addEventPrivate(Long userId, NewEventDto newEventDto) {
        log.info("Добавление события {} пользователем {}", newEventDto, userId);

        if (newEventDto.getParticipantLimit() != null && newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Participant limit must be positive or zero");
        }

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least two hours in the future");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Пользователь " +
                "с id = " + userId + " не найден"));

        Long categoryId = newEventDto.getCategory();
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("Категория " +
                "с id = " + categoryId + " не найдена"));

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        return eventMapper.toEventFullDto(eventRepository.save(event));
    }
}