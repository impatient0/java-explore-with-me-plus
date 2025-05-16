package ru.practicum.explorewithme.main.service;

import java.time.LocalDateTime;
import java.util.List;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.models.EventState;

public interface EventService {
    List<EventFullDto> getEventsAdmin(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        int from,
        int size
    );
}