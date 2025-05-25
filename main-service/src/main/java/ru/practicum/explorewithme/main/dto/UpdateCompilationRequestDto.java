package ru.practicum.explorewithme.main.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompilationRequestDto {
    private Boolean pinned;
    @Size(min = 1, max = 50, message = "Название подборки должно быть от 1 до 50 символов")
    private String title;
    private List<Long> events;
}