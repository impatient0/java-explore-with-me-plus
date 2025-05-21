package ru.practicum.explorewithme.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.main.model.ParticipationRequest;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean findByEvent_IdAndRequester_Id(Long requestEventId, Long userId);

    int countByEvent_IdAndStatusEquals(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByRequester_Id(Long userId);
}