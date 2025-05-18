
package ru.practicum.explorewithme.main.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.explorewithme.main.dto.NewUserRequestDto;
import ru.practicum.explorewithme.main.dto.UserDto;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Маппер пользователей должен")
class UserMapperTest {

    // Получаем фактическую реализацию маппера, сгенерированную MapStruct
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Nested
    @DisplayName("при преобразовании User в UserShortDto")
    class ToShortDtoTests {

        @Test
        @DisplayName("корректно маппить все поля")
        void toShortDto_ShouldMapAllFields() {
            // Подготовка
            User user = new User();
            user.setId(1L);
            user.setName("Тестовый пользователь");
            user.setEmail("test@example.com");

            // Действие
            UserShortDto result = userMapper.toShortDto(user);

            // Проверка
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(user.getId());
            assertThat(result.getName()).isEqualTo(user.getName());
        }

        @Test
        @DisplayName("возвращать null при преобразовании null")
        void toShortDto_ShouldReturnNullWhenUserIsNull() {
            // Действие
            UserShortDto result = userMapper.toShortDto(null);

            // Проверка
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("при преобразовании User в UserDto")
    class ToUserDtoTests {

        @Test
        @DisplayName("корректно маппить все поля")
        void toUserDto_ShouldMapAllFields() {
            // Подготовка
            User user = new User();
            user.setId(1L);
            user.setName("Тестовый пользователь");
            user.setEmail("test@example.com");

            // Действие
            UserDto result = userMapper.toUserDto(user);

            // Проверка
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(user.getId());
            assertThat(result.getName()).isEqualTo(user.getName());
            assertThat(result.getEmail()).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("возвращать null при преобразовании null")
        void toUserDto_ShouldReturnNullWhenUserIsNull() {
            // Действие
            UserDto result = userMapper.toUserDto(null);

            // Проверка
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("при преобразовании NewUserRequestDto в User")
    class ToUserTests {

        @Test
        @DisplayName("корректно маппить все поля")
        void toUser_ShouldMapAllFields() {
            // Подготовка
            NewUserRequestDto request = new NewUserRequestDto();
            request.setName("Тестовый пользователь");
            request.setEmail("test@example.com");

            // Действие
            User result = userMapper.toUser(request);

            // Проверка
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(request.getName());
            assertThat(result.getEmail()).isEqualTo(request.getEmail());
            // Id не должен быть установлен маппером
            assertThat(result.getId()).isNull();
        }

        @Test
        @DisplayName("возвращать null при преобразовании null")
        void toUser_ShouldReturnNullWhenNewUserRequestIsNull() {
            // Действие
            User result = userMapper.toUser(null);

            // Проверка
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("при сквозных тестах маппинга")
    class IntegrationTests {

        @Test
        @DisplayName("сохранять все поля при цепочке преобразований")
        void mapper_ShouldPreserveAllFieldsInConversionChain() {
            // Подготовка
            NewUserRequestDto request = new NewUserRequestDto();
            request.setName("Тестовый пользователь");
            request.setEmail("test@example.com");

            // Преобразование NewUserRequestDto -> User
            User user = userMapper.toUser(request);
            user.setId(1L); // устанавливаем id вручную, так как он не устанавливается маппером

            // Преобразование User -> UserDto
            UserDto userDto = userMapper.toUserDto(user);

            // Проверка полного цикла преобразования
            assertThat(userDto.getId()).isEqualTo(user.getId());
            assertThat(userDto.getName()).isEqualTo(request.getName());
            assertThat(userDto.getEmail()).isEqualTo(request.getEmail());
        }

        @Test
        @DisplayName("корректно преобразовывать в UserShortDto сохраняя нужные поля")
        void mapper_ShouldCorrectlyMapToShortDto() {
            // Подготовка
            NewUserRequestDto request = new NewUserRequestDto();
            request.setName("Тестовый пользователь");
            request.setEmail("test@example.com");

            // Преобразование NewUserRequestDto -> User -> UserShortDto
            User user = userMapper.toUser(request);
            user.setId(1L);
            UserShortDto shortDto = userMapper.toShortDto(user);

            // Проверка
            assertThat(shortDto.getId()).isEqualTo(user.getId());
            assertThat(shortDto.getName()).isEqualTo(request.getName());
            // Email не должен присутствовать в ShortDto
        }
    }

    @Nested
    @DisplayName("при работе с граничными случаями")
    class EdgeCasesTests {

        @Test
        @DisplayName("корректно обрабатывать пустые строки")
        void mapper_ShouldHandleEmptyStrings() {
            // Подготовка
            NewUserRequestDto request = new NewUserRequestDto();
            request.setName("");
            request.setEmail("");

            // Действие
            User user = userMapper.toUser(request);

            // Проверка
            assertThat(user).isNotNull();
            assertThat(user.getName()).isEmpty();
            assertThat(user.getEmail()).isEmpty();
        }

        @Test
        @DisplayName("корректно обрабатывать специальные символы")
        void mapper_ShouldHandleSpecialCharacters() {
            // Подготовка
            String specialName = "Имя с !@#$%^&*()_+";
            String specialEmail = "special!@example.com";

            NewUserRequestDto request = new NewUserRequestDto();
            request.setName(specialName);
            request.setEmail(specialEmail);

            // Действие
            User user = userMapper.toUser(request);
            UserDto userDto = userMapper.toUserDto(user);

            // Проверка
            assertThat(userDto.getName()).isEqualTo(specialName);
            assertThat(userDto.getEmail()).isEqualTo(specialEmail);
        }
    }
}