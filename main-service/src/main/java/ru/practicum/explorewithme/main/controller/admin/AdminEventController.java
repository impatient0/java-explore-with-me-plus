package ru.practicum.explorewithme.main.controller.admin;

import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMAT_PATTERN;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.UpdateEventAdminRequestDto;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.service.EventService;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminEventController {

    private final EventService eventService;
    private static final String DATETIME_FORMAT = DATE_TIME_FORMAT_PATTERN;

    /**
     * Поиск событий администратором.
     * Эндпоинт возвращает полную информацию обо всех событиях подходящих под переданные условия.
     * В случае, если по заданным фильтрам не найдено ни одного события, возвращает пустой список.
     *
     * @param users      список id пользователей, чьи события нужно найти
     * @param states     список состояний в которых находятся искомые события
     * @param categories список id категорий в которых будет вестись поиск
     * @param rangeStart дата и время не раньше которых должно произойти событие
     * @param rangeEnd   дата и время не позже которых должно произойти событие
     * @param from       количество событий, которые нужно пропустить для формирования текущего набора
     * @param size       количество событий в наборе
     * @return Список EventFullDto
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> searchEventsAdmin(
        @RequestParam(name = "users", required = false) List<Long> users,
        @RequestParam(name = "states", required = false) List<EventState> states,
        @RequestParam(name = "categories", required = false) List<Long> categories,
        @RequestParam(name = "rangeStart", required = false)
        @DateTimeFormat(pattern = DATETIME_FORMAT) LocalDateTime rangeStart,
        @RequestParam(name = "rangeEnd", required = false)
        @DateTimeFormat(pattern = DATETIME_FORMAT) LocalDateTime rangeEnd,
        @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
        @RequestParam(name = "size", defaultValue = "10") @Positive int size) {

        log.info("Admin: Received request to search events with params: users={}, states={}, categories={}, " +
                "rangeStart={}, rangeEnd={}, from={}, size={}",
            users, states, categories, rangeStart, rangeEnd, from, size);

        AdminEventSearchParams params = AdminEventSearchParams.builder().users(users).states(states)
            .categories(categories).rangeStart(rangeStart).rangeEnd(rangeEnd).build();

        List<EventFullDto> foundEvents = eventService.getEventsAdmin(
            params,
            from,
            size
        );
        log.info("Admin: Found {} events for the given criteria.", foundEvents.size());
        return foundEvents;
    }

    /**
     * Редактирование данных события и его статуса (отклонение/публикация) администратором.<br>
     * Валидация данных не требуется (согласно старому ТЗ, но DTO содержит аннотации валидации).<br>
     * Обратите внимание:
     * <ul>
     *     <li>дата начала изменяемого события должна быть не ранее чем за час от даты публикации. (Ожидается код ошибки 409)</li>
     *     <li>событие можно публиковать, только если оно в состоянии ожидания публикации (Ожидается код ошибки 409)</li>
     *     <li>событие можно отклонить, только если оно еще не опубликовано (Ожидается код ошибки 409)</li>
     * </ul>
     *
     * @param eventId                   ID события
     * @param updateEventAdminRequestDto Данные для изменения информации о событии
     * @return Обновленное EventFullDto
     */
    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto moderateEventByAdmin(
        @PathVariable Long eventId,
        @Valid @RequestBody UpdateEventAdminRequestDto updateEventAdminRequestDto) {

        log.info("Admin: Received request to moderate event id={} with data: {}",
            eventId, updateEventAdminRequestDto);

        EventFullDto moderatedEvent = eventService.moderateEventByAdmin(eventId, updateEventAdminRequestDto);

        log.info("Admin: Event id={} moderated successfully. New state: {}",
            eventId, moderatedEvent.getState());
        return moderatedEvent;
    }
}