package ru.practicum.explorewithme.main.service.params;

import lombok.*;
import org.springframework.data.domain.Sort;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@AllArgsConstructor
public class PublicCommentParameters {
    private final int from;
    private final int size;
    private final Sort sort;
}
