package ru.practicum.explorewithme.main.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@EntityListeners(AuditingEntityListener.class)
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дата и время создания запроса
     */
    @CreatedDate
    @Column(name = "created", nullable = false)
    private LocalDateTime created;

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

