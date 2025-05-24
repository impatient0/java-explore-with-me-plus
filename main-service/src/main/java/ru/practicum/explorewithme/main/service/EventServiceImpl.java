package ru.practicum.explorewithme.main.service;

import com.querydsl.core.BooleanBuilder;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
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
import ru.practicum.explorewithme.main.mapper.EventMapper;
import ru.practicum.explorewithme.main.model.Category;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.model.QEvent;
import ru.practicum.explorewithme.main.model.User;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private static final long MIN_HOURS_BEFORE_PUBLICATION_FOR_ADMIN = 1;

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

        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Range start cannot be after range end");
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

        return eventPage.isEmpty() ? Collections.emptyList() : eventMapper.toEventFullDtoList(eventPage.getContent());
    }

    @Override
    public EventFullDto moderateEventByAdmin(Long eventId, UpdateEventAdminRequestDto requestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "id", eventId));

        if (requestDto.getAnnotation() != null) {
            event.setAnnotation(requestDto.getAnnotation());
        }
        if (requestDto.getCategory() != null) {
            Category category = categoryRepository.findById(requestDto.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category", "id", requestDto.getCategory()));
            event.setCategory(category);
        }
        if (requestDto.getDescription() != null) {
            event.setDescription(requestDto.getDescription());
        }
        if (requestDto.getEventDate() != null) {
            if (requestDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BusinessRuleViolationException("Event date must be at least 2 hours in the future");
            }
            event.setEventDate(requestDto.getEventDate());
        }
        if (requestDto.getLocation() != null) {
            event.setLocation(requestDto.getLocation());
        }
        if (requestDto.getPaid() != null) {
            event.setPaid(requestDto.getPaid());
        }
        if (requestDto.getParticipantLimit() != null) {
            if (requestDto.getParticipantLimit() < 0) {
                throw new BusinessRuleViolationException("Participant limit cannot be negative");
            }
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
                        throw new BusinessRuleViolationException("Event must be in PENDING state to publish");
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLICATION_FOR_ADMIN))) {
                        throw new BusinessRuleViolationException("Event date must be at least 1 hour in the future");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new BusinessRuleViolationException("Cannot reject already published event");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown state action: " + requestDto.getStateAction());
            }
        }

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByOwner(Long userId, int from, int size) {
        if (!userRepository.existsById(userId)) {
            return Collections.emptyList();
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "eventDate"));
        Page<Event> eventPage = eventRepository.findByInitiatorId(userId, pageable);
        return eventPage.isEmpty() ? Collections.emptyList() : eventMapper.toEventShortDtoList(eventPage.getContent());
    }

    @Override
    public EventFullDto updateEventByOwner(Long userId, Long eventId, UpdateEventUserRequestDto requestDto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "id", eventId));

        if (!(event.getState() == EventState.PENDING || event.getState() == EventState.CANCELED)) {
            throw new BusinessRuleViolationException("Only PENDING or CANCELED events can be updated");
        }

        if (requestDto.getAnnotation() != null) {
            event.setAnnotation(requestDto.getAnnotation());
        }
        if (requestDto.getCategory() != null) {
            Category category = categoryRepository.findById(requestDto.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category", "id", requestDto.getCategory()));
            event.setCategory(category);
        }
        if (requestDto.getDescription() != null) {
            event.setDescription(requestDto.getDescription());
        }
        if (requestDto.getEventDate() != null) {
            if (requestDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BusinessRuleViolationException("Event date must be at least 2 hours in the future");
            }
            event.setEventDate(requestDto.getEventDate());
        }
        if (requestDto.getLocation() != null) {
            event.setLocation(requestDto.getLocation());
        }
        if (requestDto.getPaid() != null) {
            event.setPaid(requestDto.getPaid());
        }
        if (requestDto.getParticipantLimit() != null) {
            if (requestDto.getParticipantLimit() < 0) {
                throw new BusinessRuleViolationException("Participant limit cannot be negative");
            }
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
                    throw new IllegalArgumentException("Unknown state action: " + requestDto.getStateAction());
            }
        }

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventPrivate(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "id", eventId));
        return eventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto addEventPrivate(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", "id", userId));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new EntityNotFoundException("Category", "id", newEventDto.getCategory()));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BusinessRuleViolationException("Event date must be at least 2 hours in the future");
        }
        if (newEventDto.getParticipantLimit() != null && newEventDto.getParticipantLimit() < 0) {
            throw new BusinessRuleViolationException("Participant limit cannot be negative");
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        event.setViews(0L);
        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    // Новый метод для увеличения счетчика просмотров
    @Transactional
    public void incrementEventViews(Long eventId, String ip) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "id", eventId));
        // Здесь можно добавить логику проверки уникальности IP, если требуется
        event.setViews(event.getViews() + 1);
        eventRepository.save(event);
    }
}