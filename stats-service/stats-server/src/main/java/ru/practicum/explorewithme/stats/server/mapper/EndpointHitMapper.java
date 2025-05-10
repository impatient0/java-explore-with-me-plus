package ru.practicum.explorewithme.stats.server.mapper;

import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.server.model.EndpointHit;

public interface EndpointHitMapper {

    /**
     * Преобразует EndpointHitDto в сущность EndpointHit.
     *
     * @param dto объект EndpointHitDto для преобразования.
     * @return сущность EndpointHit, или null если dto равен null.
     */
    EndpointHit toEndpointHit(EndpointHitDto dto);

    /**
     * Преобразует сущность EndpointHit в EndpointHitDto.
     *
     * @param entity сущность EndpointHit для преобразования.
     * @return объект EndpointHitDto, или null если entity равен null.
     */
    EndpointHitDto toEndpointHitDto(EndpointHit entity);

}