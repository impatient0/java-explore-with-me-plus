package ru.practicum.explorewithme.main.service;

import java.util.List;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;
import ru.practicum.explorewithme.main.dto.NewEventDto;

public interface EventService {
    List<EventFullDto> getEventsAdmin(
        AdminEventSearchParams params,
        int from,
        int size
    );

    EventFullDto addEventPrivate(Long userId, NewEventDto newEventDto);
}
