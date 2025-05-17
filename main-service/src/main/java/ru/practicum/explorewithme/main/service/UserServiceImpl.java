package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.NewUserRequest;
import ru.practicum.explorewithme.main.dto.UserDto;
import ru.practicum.explorewithme.main.error.EntityAlreadyExistsException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.UserMapper;
import ru.practicum.explorewithme.main.model.User;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.GetListUsersParameters;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserDto) {

        if (userRepository.existsByEmail(newUserDto.getEmail())) {
            throw new EntityAlreadyExistsException("User", "email", newUserDto.getEmail());
        }

        return userMapper.toUserDto(userRepository.save(userMapper.toUser(newUserDto)));
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, NewUserRequest updateUserDto) {

        Optional<User> existingUser = userRepository.findById(userId);
        User newUser = new User();

        if (!existingUser.isPresent()) {
            throw new EntityNotFoundException("User", "Id", userId);
        } else {
            newUser = existingUser.get();
        }

        if (updateUserDto.getEmail() != null && !updateUserDto.getEmail().equals(newUser.getEmail())) {
            if (userRepository.existsByEmail(updateUserDto.getEmail())) {
                throw new EntityAlreadyExistsException("User", "email", updateUserDto.getEmail());
            }
            newUser.setEmail(updateUserDto.getEmail());
        }

        if (updateUserDto.getName() != null) {
            newUser.setName(updateUserDto.getName());
        }

        return userMapper.toUserDto(userRepository.save(newUser));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {

        Optional<User> existingUser = userRepository.findById(userId);

        if (!existingUser.isPresent()) {
            throw new EntityNotFoundException("User", "Id", userId);
        }

        userRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(GetListUsersParameters parameters) {

        Pageable pageable = PageRequest.of(parameters.getFrom() / parameters.getSize(),
                parameters.getSize());

        List<UserDto> result;

        if (parameters.getIds() == null || parameters.getIds().isEmpty()) {
            result = userRepository.findAll(pageable).stream()
                    .map(userMapper::toUserDto)
                    .collect(Collectors.toList());
        } else {
            result = userRepository.findAllByIdIn(parameters.getIds(), pageable).stream()
                    .map(userMapper::toUserDto)
                    .collect(Collectors.toList());
        }

        return result;
    }

}
