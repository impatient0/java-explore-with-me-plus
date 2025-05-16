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
import ru.practicum.explorewithme.main.mapper.EventMapper;
import ru.practicum.explorewithme.main.models.Event;
import ru.practicum.explorewithme.main.models.EventState;
import ru.practicum.explorewithme.main.models.QEvent;
import ru.practicum.explorewithme.main.repository.EventRepository;
// import ru.practicum.explorewithme.main.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    // private final UserRepository userRepository;
    // private final CategoryRepository categoryRepository;

    @Override
    public List<EventFullDto> getEventsAdmin(List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        int from,
        int size) {

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

        Page<Event> eventPage = eventRepository.findAll(finalPredicate, pageable);

        if (eventPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventFullDto> result = eventMapper.toEventFullDtoList(eventPage.getContent());
        log.debug("Admin search found {} events on page {}/{}", result.size(), pageable.getPageNumber(), eventPage.getTotalPages());
        return result;
    }

}