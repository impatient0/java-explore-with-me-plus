package ru.practicum.explorewithme.main.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "events")
@EqualsAndHashCode(of = {"id", "title"})
public class Compilation {

    /**
     * Уникальный идентификатор подборки.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Флаг, закреплена ли подборка на главной странице.
     */
    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    /**
     * Название подборки.
     */
    @Column(name = "title", nullable = false, unique = true, length = 128)
    private String title;

    /**
     * События, входящие в подборку.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private Set<Event> events = new HashSet<>();
}
