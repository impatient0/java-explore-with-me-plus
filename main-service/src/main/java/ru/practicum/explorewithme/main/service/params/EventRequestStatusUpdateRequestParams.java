package ru.practicum.explorewithme.main.service.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequestParams {
    Long userId;
    Long eventId;
    List<Long> requestIds;
    RequestStatus status;
}
