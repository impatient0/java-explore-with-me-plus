package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.NewUserRequest;
import ru.practicum.explorewithme.main.dto.UserDto;
import ru.practicum.explorewithme.main.service.params.GetListUsersParameters;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserDto);
    UserDto updateUser(Long userId, NewUserRequest updateUserDto);
    void deleteUser(Long userId);
    List<UserDto> getUsers(GetListUsersParameters parameters);
}

