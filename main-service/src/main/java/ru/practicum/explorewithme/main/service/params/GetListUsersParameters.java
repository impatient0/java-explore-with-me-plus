package ru.practicum.explorewithme.main.service.params;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetListUsersParameters {
    List<Long> ids;
    int from;
    int size;
}
