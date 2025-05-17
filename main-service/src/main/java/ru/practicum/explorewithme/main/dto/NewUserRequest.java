package ru.practicum.explorewithme.main.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "Имя должно быть от 2 до 250 символов")
    @Size(min=2, max=250, message = "Имя должно быть от 2 до 250 символов")
    private String name;

    @NotBlank(message = "Email не может быть пустым")
    @Size(min=6, max=254, message = "Email должен быть от 6 до 254 символов")
    @Email(message = "Некорректный формат email")
    private String email;
}
