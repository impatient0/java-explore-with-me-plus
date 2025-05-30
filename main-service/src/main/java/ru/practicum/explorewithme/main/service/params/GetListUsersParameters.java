package ru.practicum.explorewithme.main.service.params;

import lombok.*;

import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class GetListUsersParameters {
    private final List<Long> ids;
    private final int from;
    private final int size;
}
