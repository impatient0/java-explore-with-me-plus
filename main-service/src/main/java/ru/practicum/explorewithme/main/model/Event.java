package ru.practicum.explorewithme.main.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"category", "initiator", "compilations"})
@EqualsAndHashCode(of = {"id", "title", "annotation", "eventDate", "publishedOn"})
@EntityListeners(AuditingEntityListener.class)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @CreatedDate
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "participant_limit", nullable = false)
    private int participantLimit;

    @Column(name = "request_moderation", nullable = false)
    private boolean requestModeration;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Embedded
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private EventState state;

    @ManyToMany(mappedBy = "events")
    @Builder.Default
    private Set<Compilation> compilations = new HashSet<>();

    @Column(name = "views", nullable = false)
    private Long views = 0L;

    @Column(name = "confirmed_requests", nullable = false)
    private Long confirmedRequests = 0L;
}