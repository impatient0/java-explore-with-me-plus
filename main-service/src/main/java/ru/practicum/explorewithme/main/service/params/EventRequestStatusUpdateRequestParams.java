package ru.practicum.explorewithme.main.service.params;

import lombok.*;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class EventRequestStatusUpdateRequestParams {
    private final Long userId;
    private final Long eventId;
    private final List<Long> requestIds;
    private final RequestStatus status;
}
