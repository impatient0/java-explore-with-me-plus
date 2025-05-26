package ru.practicum.explorewithme.main.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.dto.NewEventDto;
import ru.practicum.explorewithme.main.model.Event;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mappings({
        @Mapping(source = "category", target = "category"),
        @Mapping(source = "initiator", target = "initiator"),
        @Mapping(source = "confirmedRequestsCount", target = "confirmedRequests")
    })
    EventFullDto toEventFullDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "compilations", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(source = "category", target = "category")
    @Mapping(target = "state", expression = "java(ru.practicum.explorewithme.main.model.EventState.PENDING)")
    Event toEvent(NewEventDto newEventDto);

    List<EventFullDto> toEventFullDtoList(List<Event> events);

    @Mappings({
        @Mapping(source = "category", target = "category"),
        @Mapping(source = "initiator", target = "initiator"),
        @Mapping(source = "confirmedRequestsCount", target = "confirmedRequests"),
        @Mapping(target = "views", ignore = true)
    })
    EventShortDto toEventShortDto(Event event);

    List<EventShortDto> toEventShortDtoList(List<Event> events);
}
