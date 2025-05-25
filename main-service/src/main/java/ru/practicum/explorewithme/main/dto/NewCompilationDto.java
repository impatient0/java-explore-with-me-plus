package ru.practicum.explorewithme.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewCompilationDto {
    @Builder.Default
    private Boolean pinned = false;
    @NotBlank(message = "Название подборки не может быть пустым")
    @Size(max = 50, message = "Название подборки должно быть до 50 символов")
    private String title;
    private List<Long> events;
}