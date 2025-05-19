package ru.practicum.explorewithme.main.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.NewEventDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.EventMapper;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.model.QEvent;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<EventFullDto> getEventsAdmin(AdminEventSearchParams params,
        int from,
        int size) {

        List<Long> users = params.getUsers();
        List<EventState> states = params.getStates();
        List<Long> categories = params.getCategories();
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        log.debug("Admin search for events with params: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
            users, states, categories, rangeStart, rangeEnd, from, size);

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Admin search: rangeStart cannot be after rangeEnd.");
        }

        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        if (users != null && !users.isEmpty()) {
            // TODO: Возможно, стоит проверить, существуют ли такие пользователи, если это требуется по логике
            predicate.and(qEvent.initiator.id.in(users));
        }

        if (states != null && !states.isEmpty()) {
            predicate.and(qEvent.state.in(states));
        }

        if (categories != null && !categories.isEmpty()) {
            // TODO: Возможно, стоит проверить, существуют ли такие категории
            predicate.and(qEvent.category.id.in(categories));
        }

        if (rangeStart != null) {
            predicate.and(qEvent.eventDate.goe(rangeStart)); // greater or equal
        }

        if (rangeEnd != null) {
            predicate.and(qEvent.eventDate.loe(rangeEnd)); // lower or equal
        }

        Predicate finalPredicate = predicate.getValue();

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Event> eventPage = eventRepository.findAll(predicate, pageable);

        if (eventPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventFullDto> result = eventMapper.toEventFullDtoList(eventPage.getContent());
        log.debug("Admin search found {} events on page {}/{}", result.size(), pageable.getPageNumber(), eventPage.getTotalPages());
        return result;
    }

    @Transactional
    @Override
    public EventFullDto addEventPrivate(Long userId, NewEventDto newEventDto) {

        log.info("Добавление события {} пользователем {}", newEventDto, userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Пользователь " +
                "с id = " + userId + " не найден"));

        Long categoryId = newEventDto.getCategory();
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new EntityNotFoundException("Категория " +
                "с id = " + categoryId + " не найдена"));

        LocalDateTime eventDate = newEventDto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BusinessRuleViolationException("Дата должна быть не ранее, чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        return eventMapper.toEventFullDto(eventRepository.save(event));
    }
}