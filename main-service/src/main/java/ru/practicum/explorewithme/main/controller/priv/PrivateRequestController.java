package ru.practicum.explorewithme.main.controller.priv;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.main.service.RequestService;


import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateRequestController {

    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(
            @PathVariable @Positive Long userId,
            @RequestParam @Positive Long requestEventId) {
        log.info("Private: Received request to add user {} in event: {}", userId, requestEventId);
        ParticipationRequestDto result = requestService.createRequest(userId, requestEventId);
        log.info("Private: Adding user: {}", result);
        return result;
    }

    @PatchMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long requestId) {
        log.info("Private: Received request user {} to cancel request with Id: {}", userId, requestId);
        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);
        log.info("Private: Cancel request: {}", result);
        return result;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getRequests(@PathVariable @Positive Long userId) {
        log.info("Private: Received request to get list participation requests for user {}", userId);
        List<ParticipationRequestDto> result = requestService.getRequests(userId);
        log.info("Private: Received list participation requests for user {}: {}", userId, result);
        return result;
    }

}