package ru.practicum.explorewithme.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.main.model.ParticipationRequest;
import ru.practicum.explorewithme.main.model.RequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByEvent_IdAndRequester_Id(Long requestEventId, Long userId);

    int countByEvent_IdAndStatusEquals(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByRequester_Id(Long userId);

    Optional<ParticipationRequest> findByIdAndRequester_Id(Long requestId, Long userId);

    List<ParticipationRequest> findAllByIdIn(List<Long> requestIdsForUpdate);

    int countByIdInAndEvent_Id(List<Long> requestIdsForUpdate, Long eventId);

    @Modifying
    @Query("UPDATE ParticipationRequest r SET r.status = ru.practicum.explorewithme.main.model.RequestStatus.REJECTED " +
            "WHERE r.event.id = :eventId AND r.status = :status")
    void updateStatusToRejected(@Param("eventId") Long eventId, @Param("status") RequestStatus status);

    List<ParticipationRequest> findByEvent_IdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByEvent_Id(Long eventId);
}