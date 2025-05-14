package ru.practicum.explorewithme.main.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"category", "initiator", "location", "compilations"})
@EqualsAndHashCode(of = {"id", "title", "annotation", "eventDate", "publishedOn"})
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
    @Column(name = "annotation", nullable = false)
    private String annotation;

    /**
     * Полное описание события
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Дата и время проведения события
     */
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /**
     * Дата и время создания события
     */
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn = LocalDateTime.now();

    /**
     * Дата и время публикации события
     */
    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    /**
     * Флаг платного участия
     */
    @Column(name = "paid", nullable = false)
    private boolean paid = false;

    /**
     * Лимит участников события (0 - без ограничений)
     */
    @Column(name = "participant_limit", nullable = false)
    private int participantLimit = 0;

    /**
     * Требуется ли модерация заявок на участие
     */
    @Column(name = "request_moderation", nullable = false)
    private boolean requestModeration = true;

    /**
     * Заголовок события
     */
    @Column(name = "title", nullable = false, length = 128)
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
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id", nullable = false)
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
    private Set<Compilation> compilations = new HashSet<>();

}
