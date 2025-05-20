package ru.practicum.explorewithme.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.explorewithme.main.dto.NewUserRequestDto;
import ru.practicum.explorewithme.main.dto.UserDto;
import ru.practicum.explorewithme.main.error.EntityAlreadyExistsException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.model.User;
import ru.practicum.explorewithme.main.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.service.params.GetListUsersParameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Интеграционное тестирование UserServiceImpl")
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16.1")
            .withDatabaseName("explorewithme_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private NewUserRequestDto newUserRequestDto;
    private NewUserRequestDto anotherUserRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        newUserRequestDto = new NewUserRequestDto();
        newUserRequestDto.setName("Тестовый пользователь");
        newUserRequestDto.setEmail("test@example.com");

        anotherUserRequest = new NewUserRequestDto();
        anotherUserRequest.setName("Другой пользователь");
        anotherUserRequest.setEmail("another@example.com");
    }

    @Nested
    @DisplayName("Создание пользователя")
    class CreateUserTests {

        @Test
        @DisplayName("Успешное создание пользователя")
        void createUser_Success() {

            UserDto createdUser = userService.createUser(newUserRequestDto);

            assertNotNull(createdUser);
            assertNotNull(createdUser.getId());
            assertEquals(newUserRequestDto.getName(), createdUser.getName());
            assertEquals(newUserRequestDto.getEmail(), createdUser.getEmail());

            Optional<User> userFromDb = userRepository.findById(createdUser.getId());
            assertTrue(userFromDb.isPresent());
            assertEquals(newUserRequestDto.getName(), userFromDb.get().getName());
            assertEquals(newUserRequestDto.getEmail(), userFromDb.get().getEmail());
        }

        @Test
        @DisplayName("Исключение при создании пользователя с дублирующимся email")
        void createUser_DuplicateEmail_ThrowsException() {

            userService.createUser(newUserRequestDto);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
                userService.createUser(newUserRequestDto);
            });

            assertTrue(exception.getMessage().contains(newUserRequestDto.getEmail()));
        }
    }

    @Nested
    @DisplayName("Удаление пользователя")
    class DeleteUserTests {

        @Test
        @DisplayName("Успешное удаление пользователя")
        void deleteUser_Success() {

            UserDto createdUser = userService.createUser(newUserRequestDto);

            assertTrue(userRepository.existsById(createdUser.getId()));

            userService.deleteUser(createdUser.getId());

            assertFalse(userRepository.existsById(createdUser.getId()));
        }

        @Test
        @DisplayName("Исключение при удалении несуществующего пользователя")
        void deleteUser_UserNotFound_ThrowsException() {

            Long nonExistentUserId = 999L;

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                userService.deleteUser(nonExistentUserId);
            });

            assertTrue(exception.getMessage().contains(nonExistentUserId.toString()));
        }
    }

    @Nested
    @DisplayName("Получение списка пользователей")
    class GetUsersTests {

        @Test
        @DisplayName("Получение всех пользователей без фильтрации по ID")
        void getUsers_WithoutIds_ReturnsAllUsers() {

            UserDto user1 = userService.createUser(newUserRequestDto);
            UserDto user2 = userService.createUser(anotherUserRequest);

            GetListUsersParameters parameters = new GetListUsersParameters(null, 0, 10);

            List<UserDto> users = userService.getUsers(parameters);

            assertNotNull(users);
            assertEquals(2, users.size());

            List<Long> userIds = Arrays.asList(users.get(0).getId(), users.get(1).getId());
            assertTrue(userIds.contains(user1.getId()));
            assertTrue(userIds.contains(user2.getId()));
        }

        @Test
        @DisplayName("Получение пользователей с фильтрацией по ID")
        void getUsers_WithIds_ReturnsSpecificUsers() {

            UserDto user1 = userService.createUser(newUserRequestDto);
            userService.createUser(anotherUserRequest); // user2 не должен попасть в выборку

            GetListUsersParameters parameters = new GetListUsersParameters(
                    Collections.singletonList(user1.getId()), 0, 10);

            List<UserDto> users = userService.getUsers(parameters);

            assertNotNull(users);
            assertEquals(1, users.size());
            assertEquals(user1.getId(), users.get(0).getId());
        }

        @Test
        @DisplayName("Корректная работа пагинации при получении пользователей")
        void getUsers_Pagination_ReturnsCorrectPage() {

            List<UserDto> createdUsers = IntStream.range(0, 5)
                    .mapToObj(i -> {
                        NewUserRequestDto request = new NewUserRequestDto();
                        request.setName("User " + i);
                        request.setEmail("user" + i + "@example.com");
                        return userService.createUser(request);
                    })
                    .collect(Collectors.toList());

            GetListUsersParameters page1Params = new GetListUsersParameters(null, 0, 2);
            List<UserDto> page1 = userService.getUsers(page1Params);

            GetListUsersParameters page2Params = new GetListUsersParameters(null, 2, 2);
            List<UserDto> page2 = userService.getUsers(page2Params);

            GetListUsersParameters page3Params = new GetListUsersParameters(null, 4, 2);
            List<UserDto> page3 = userService.getUsers(page3Params);

            assertEquals(2, page1.size());
            assertEquals(2, page2.size());
            assertEquals(1, page3.size());

            List<Long> allUserIds = new java.util.ArrayList<>();
            allUserIds.addAll(page1.stream().map(UserDto::getId).collect(Collectors.toList()));
            allUserIds.addAll(page2.stream().map(UserDto::getId).collect(Collectors.toList()));
            allUserIds.addAll(page3.stream().map(UserDto::getId).collect(Collectors.toList()));

            assertEquals(5, allUserIds.size());
            assertEquals(5, allUserIds.stream().distinct().count());
        }

        @Test
        @DisplayName("Получение пустого списка при отсутствии пользователей")
        void getUsers_EmptyRepository_ReturnsEmptyList() {

            GetListUsersParameters parameters = new GetListUsersParameters(null, 0, 10);
            List<UserDto> users = userService.getUsers(parameters);

            assertNotNull(users);
            assertTrue(users.isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты производительности")
    class PerformanceTests {

        @Test
        @DisplayName("Эффективная работа с большим количеством данных")
        void getUsers_WithLargeDataset_PerformsEfficiently() {

            for (int i = 0; i < 100; i++) {
                NewUserRequestDto request = new NewUserRequestDto();
                request.setName("User " + i);
                request.setEmail("user" + i + "@example.com");
                userService.createUser(request);
            }

            long startTime = System.currentTimeMillis();
            GetListUsersParameters parameters = new GetListUsersParameters(null, 0, 50);
            List<UserDto> users = userService.getUsers(parameters);
            long endTime = System.currentTimeMillis();

            assertEquals(50, users.size());
            assertTrue((endTime - startTime) < 1000); // Ожидаем выполнение менее чем за секунду

            System.out.println("Время выполнения запроса для 50 пользователей из 100: " + (endTime - startTime) + " мс");
        }
    }

    @Nested
    @DisplayName("Тесты обработки граничных случаев")
    class EdgeCaseTests {

        @Test
        @DisplayName("Корректная обработка запроса страницы за пределами допустимого диапазона")
        void getUsers_PageOutOfRange_ReturnsEmptyList() {

            IntStream.range(0, 3)
                    .forEach(i -> {
                        NewUserRequestDto request = new NewUserRequestDto();
                        request.setName("User " + i);
                        request.setEmail("user" + i + "@example.com");
                        userService.createUser(request);
                    });

            GetListUsersParameters outOfRangeParams = new GetListUsersParameters(null, 10, 5);
            List<UserDto> result = userService.getUsers(outOfRangeParams);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

    }
}