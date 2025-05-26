package ru.practicum.explorewithme.main.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewUserRequestDto {
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 250, message = "Имя должно быть от 2 до 250 символов")
    String name;

    @NotBlank(message = "Email не может быть пустым")
    @Size(min = 6, max = 254, message = "Email должен быть от 6 до 254 символов")
    @Email(message = "Некорректный формат email")
    String email;
}
