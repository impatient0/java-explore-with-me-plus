package ru.practicum.explorewithme.main.mapper;

import org.mapstruct.Mapper;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserShortDto toShortDto(User user);
}
