package ru.practicum.explorewithme.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.NewUserRequestDto;
import ru.practicum.explorewithme.main.dto.UserDto;
import ru.practicum.explorewithme.main.error.EntityAlreadyExistsException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@DisplayName("Контроллер администрирования пользователей должен")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private NewUserRequestDto newUserRequestDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        newUserRequestDto = new NewUserRequestDto();
        newUserRequestDto.setName("Тестовый пользователь");
        newUserRequestDto.setEmail("test@example.com");

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("Тестовый пользователь");
        userDto.setEmail("test@example.com");
    }

    @Nested
    @DisplayName("при создании пользователя")
    class CreateUserTests {

        @Test
        @DisplayName("возвращать созданного пользователя со статусом 201")
        void createUser_ReturnsCreatedUser() throws Exception {
            when(userService.createUser(any(NewUserRequestDto.class))).thenReturn(userDto);

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserRequestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Тестовый пользователь")))
                    .andExpect(jsonPath("$.email", is("test@example.com")));

            verify(userService, times(1)).createUser(any(NewUserRequestDto.class));
        }

        @Test
        @DisplayName("возвращать 409 при попытке создать пользователя с существующим email")
        void createUser_WithExistingEmail_ReturnsConflict() throws Exception {
            when(userService.createUser(any(NewUserRequestDto.class)))
                    .thenThrow(new EntityAlreadyExistsException("Пользователь с email test@example.com уже существует"));

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserRequestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("уже существует")));

            verify(userService, times(1)).createUser(any(NewUserRequestDto.class));
        }

        @Test
        @DisplayName("возвращать 400 при создании пользователя с невалидными данными")
        void createUser_WithInvalidData_ReturnsBadRequest() throws Exception {
            NewUserRequestDto invalidRequest = new NewUserRequestDto();
            // Email и имя не заданы

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).createUser(any(NewUserRequestDto.class));
        }

        @Test
        @DisplayName("возвращать корректный заголовок Content-Type в ответе")
        void createUser_WithValidRequest_HasCorrectContentTypeHeader() throws Exception {
            when(userService.createUser(any())).thenReturn(userDto);

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserRequestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                    .andExpect(jsonPath("$.id", is(1)));

            verify(userService, times(1)).createUser(any());
        }

    }

    @Nested
    @DisplayName("при удалении пользователя")
    class DeleteUserTests {

        @Test
        @DisplayName("возвращать статус 204 без тела ответа")
        void deleteUser_ReturnsNoContent() throws Exception {
            doNothing().when(userService).deleteUser(anyLong());

            mockMvc.perform(delete("/admin/users/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(userService, times(1)).deleteUser(1L);
        }

        @Test
        @DisplayName("возвращать 404 при удалении несуществующего пользователя")
        void deleteUser_WithNonExistingId_ReturnsNotFound() throws Exception {
            doThrow(new EntityNotFoundException("Пользователь","Id", 999L))
                    .when(userService).deleteUser(999L);

            mockMvc.perform(delete("/admin/users/999"))
                    .andExpect(status().isNotFound());

            verify(userService, times(1)).deleteUser(999L);
        }
    }

    @Nested
    @DisplayName("при получении пользователей")
    class GetUsersTests {

        @Test
        @DisplayName("возвращать список всех пользователей без фильтрации по id")
        void getUsers_WithoutIds_ReturnsAllUsers() throws Exception {
            List<UserDto> users = Arrays.asList(
                    userDto,
                    createUserDto(2L, "Второй пользователь", "user2@example.com")
            );

            when(userService.getUsers(any())).thenReturn(users);

            mockMvc.perform(get("/admin/users")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].id", is(2)));

            verify(userService, times(1)).getUsers(any());
        }

        @Test
        @DisplayName("возвращать отфильтрованный список пользователей при указании id")
        void getUsers_WithIds_ReturnsFilteredUsers() throws Exception {
            List<UserDto> users = Collections.singletonList(userDto);

            when(userService.getUsers(any())).thenReturn(users);

            mockMvc.perform(get("/admin/users")
                            .param("ids", "1")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(1)));

            verify(userService, times(1)).getUsers(any());
        }

        @Test
        @DisplayName("возвращать 400 при невалидных параметрах пагинации")
        void getUsers_WithInvalidPaginationParams_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/admin/users")
                            .param("from", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).getUsers(any());
        }

        @Test
        @DisplayName("возвращать корректный заголовок Content-Type в ответе")
        void getUsers_ResponseHasCorrectContentTypeHeader() throws Exception {
            List<UserDto> users = Arrays.asList(userDto);
            when(userService.getUsers(any())).thenReturn(users);

            mockMvc.perform(get("/admin/users")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(userService, times(1)).getUsers(any());
        }
    }

    @Nested
    @DisplayName("при обработке невалидного JSON")
    class InvalidJsonTests {

        @Test
        @DisplayName("возвращать 400 при синтаксически некорректном JSON")
        void request_WithInvalidJson_ReturnsBadRequest() throws Exception {
            String invalidJson = "{\"name\":\"Тестовый пользователь\", \"email\":\"invalid-json";

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.reason", containsString("Malformed JSON")));

            verify(userService, never()).createUser(any());
        }

        @Test
        @DisplayName("возвращать 400 при некорректном JSON-массиве")
        void request_WithMalformedJsonArray_ReturnsBadRequest() throws Exception {
            String invalidJsonArray = "[{\"name\":\"Тестовый пользователь\",}]"; // Ошибка - лишняя запятая

            mockMvc.perform(post("/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJsonArray))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).createUser(any());
        }
    }

    // Вспомогательный метод для создания UserDto
    private UserDto createUserDto(Long id, String name, String email) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }
}