package ru.practicum.explorewithme.main.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequestDto {
    @Size(min = 1, max = 128, message = "Название подборки должно быть от 1 до 128 символов")
    private String title;

    private Boolean pinned;

    private List<Long> events;
}
