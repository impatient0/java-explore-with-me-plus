package ru.practicum.explorewithme.main.controller.priv;

import jakarta.validation.Valid;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.dto.NewEventDto;
import ru.practicum.explorewithme.main.service.EventService;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateEventController {

    private final EventService eventService;

    /**
     * Получение событий, добавленных текущим пользователем.
     * В случае, если по заданным фильтрам не найдено ни одного события, возвращает пустой список.
     *
     * @param userId ID текущего пользователя
     * @param from   количество элементов, которые нужно пропустить для формирования текущего набора
     * @param size   количество элементов в наборе
     * @return Список EventShortDto
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventsAddedByCurrentUser(
        @PathVariable Long userId,
        @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
        @RequestParam(name = "size", defaultValue = "10") @Positive int size) {

        log.info("User id={}: Received request to get own events, from={}, size={}", userId, from, size);
        List<EventShortDto> events = eventService.getEventsByOwner(userId, from, size);
        log.info("User id={}: Found {} events. From={}, size={}", userId, events.size(), from, size);
        return events;
    }

    /**
     * Получение полной информации о событии, добавленном текущим пользователем.
     * В случае, если события с заданным id не найдено, возвращает статус код 404.
     *
     * @param userId  ID текущего пользователя
     * @param eventId ID события
     * @return EventFullDto с полной информацией о событии
     */
    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getFullEventInfoByOwner(
        @PathVariable Long userId,
        @PathVariable Long eventId) {

        log.info("User id={}: Received request to get full info for event id={}", userId, eventId);
        EventFullDto eventFullDto = eventService.getEventPrivate(userId, eventId);
        log.info("User id={}: Found full info for event id={}: {}", userId, eventId, eventFullDto.getId());
        return eventFullDto;
    }

    @PostMapping
    public ResponseEntity<EventFullDto> addEventPrivate(@PathVariable Long userId, @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Создание нового события {} зарегистрированным пользователем c id {}", newEventDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.addEventPrivate(userId, newEventDto));
    }

    // TODO: POST /users/{userId}/events - Добавление нового события (NewEventDto -> EventFullDto)
    // (Задача: PRIVATE-EVENTS: Добавление нового события)

    // TODO: PATCH /users/{userId}/events/{eventId} - Изменение события, добавленного текущим пользователем (UpdateEventUserRequest -> EventFullDto)
    // (Задача: PRIVATE-EVENTS: Изменение события, добавленного текущим пользователем)

    // TODO: GET /users/{userId}/events/{eventId}/requests - Получение запросов на участие в событии текущего пользователя (-> List<ParticipationRequestDto>)
    // (Задача: PRIVATE-EVENTS: Получение запросов на участие в событии текущего пользователя)

    // TODO: PATCH /users/{userId}/events/{eventId}/requests - Изменение статуса заявок (подтверждение/отклонение) (EventRequestStatusUpdateRequest -> EventRequestStatusUpdateResult)
    // (Задача: PRIVATE-EVENTS: Изменение статуса заявок (подтверждение/отклонение))
}