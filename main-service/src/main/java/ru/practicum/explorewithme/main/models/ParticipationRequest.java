package ru.practicum.explorewithme.main.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests", uniqueConstraints = {
        @UniqueConstraint(name = "unique_requester_event", columnNames = {"requester_id", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"requester", "event"})
@EqualsAndHashCode(of = {"id", "created"})
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дата и время создания запроса
     */
    @Column(name = "created", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    /**
     * Пользователь, создавший запрос на участие
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /**
     * Событие, на которое пользователь хочет попасть
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Статус запроса (PENDING, CONFIRMED, REJECTED, CANCELED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status;
}

