package ru.practicum.explorewithme.main.controller.priv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.*;
import ru.practicum.explorewithme.main.service.EventService;
import ru.practicum.explorewithme.main.service.RequestService;
import ru.practicum.explorewithme.main.service.params.EventRequestStatusUpdateRequestParams;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateEventController {

    private final EventService eventService;
    private final RequestService requestService;

    @PostMapping("/{userId}/events")
    public ResponseEntity<EventFullDto> addEventPrivate(@PathVariable Long userId, @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Создание нового события {} зарегистрированным пользователем c id {}", newEventDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.addEventPrivate(userId, newEventDto));
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResultDto updateRequestsStatus(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequestDto requestStatusUpdate) {
        log.info("Private: Received request to change status requests {} for event {} when initiator {}",
                requestStatusUpdate.getRequestIds(), eventId, userId);
        EventRequestStatusUpdateRequestParams requestParams = EventRequestStatusUpdateRequestParams.builder()
                .userId(userId)
                .eventId(eventId)
                .requestIds(requestStatusUpdate.getRequestIds())
                .status(requestStatusUpdate.getStatus())
                .build();
        EventRequestStatusUpdateResultDto result = requestService.updateRequestsStatus(requestParams);
        log.info("Private: Received list requests for event {} when initiator {} : {}", eventId, userId, result);
        return result;
    }

}
