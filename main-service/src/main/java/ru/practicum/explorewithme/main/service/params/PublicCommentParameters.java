package ru.practicum.explorewithme.main.service.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PublicCommentParameters {
    private final int from;
    private final int size;
    private final String sort;
}
