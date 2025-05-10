package ru.practicum.explorewithme.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointHitDto {

    @NotBlank(message = "Поле app не может быть пустым")
    @Size(min = 1, max = 32, message = "Поле app должно быть от 1 до 32 символов")
    private String app;

    @NotBlank(message = "Поле uri не может быть пустым")
    @Size(min = 1, max = 128, message = "Поле uri должно быть от 1 до 128 символов")
    private String uri;

    @NotBlank(message = "Поле ip не может быть пустым")
    @Size(min = 7, max = 16, message = "Поле ip должно быть от 7 до 16 символов")
    private String ip;

    @NotNull(message = "Поле timestamp не может быть пустым")
    @PastOrPresent(message = "Поле timestamp должно быть не позже текущей даты и времени")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}