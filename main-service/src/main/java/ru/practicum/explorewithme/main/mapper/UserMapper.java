package ru.practicum.explorewithme.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.explorewithme.main.dto.NewUserRequestDto;
import ru.practicum.explorewithme.main.dto.UserDto;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserShortDto toShortDto(User user);

    UserDto toUserDto(User user);

    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequestDto newUserDto);

}
