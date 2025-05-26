package ru.practicum.explorewithme.main.service.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class EventRequestStatusUpdateRequestParams {
    private final Long userId;
    private final Long eventId;
    private final List<Long> requestIds;
    private final RequestStatus status;
}
