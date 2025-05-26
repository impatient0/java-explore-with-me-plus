package ru.practicum.explorewithme.main.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Formula;
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

    /**
     * Уникальный идентификатор события
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Краткая аннотация события
     */
    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    /**
     * Полное описание события
     */
    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    /**
     * Дата и время проведения события
     */
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /**
     * Дата и время создания события
     */
    @CreatedDate
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    /**
     * Дата и время публикации события
     */
    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    /**
     * Флаг платного участия
     */
    @Column(name = "paid", nullable = false)
    private boolean paid;

    /**
     * Лимит участников события (0 - без ограничений)
     */
    @Column(name = "participant_limit", nullable = false)
    private int participantLimit;

    /**
     * Требуется ли модерация заявок на участие
     */
    @Column(name = "request_moderation", nullable = false)
    private boolean requestModeration;

    /**
     * Заголовок события
     */
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    /**
     * Категория события
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Инициатор события
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    /**
     * Местоположение события
     */
    @Embedded
    private Location location;

    /**
     * Текущее состояние события
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private EventState state;

    /**
     *  Список подборок, в которых присутствует событие (создано для корректной обратной выборки)
     */
    @ManyToMany(mappedBy = "events")
    @Builder.Default
    private Set<Compilation> compilations = new HashSet<>();

    /**
     *  Количество подтверждённых заявок
     */
    @Formula("(SELECT COUNT(r.id) FROM requests r WHERE r.event_id = id AND r.status = 'CONFIRMED')")
    private Long confirmedRequestsCount;

}
