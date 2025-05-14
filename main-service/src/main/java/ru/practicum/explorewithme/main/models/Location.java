package ru.practicum.explorewithme.main.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id", "title", "lat", "lon"})
public class Location {

    /**
     * Уникальный идентификатор локации.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальное название локации.
     */
    @Column(name = "title", nullable = false, length = 128, unique = true)
    private String title;

    /**
     * Широта географической точки.
     */
    @Column(name = "lat")
    private Float lat;

    /**
     * Долгота географической точки.
     */
    @Column(name = "lon")
    private Float lon;

}
