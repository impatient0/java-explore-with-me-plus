package ru.practicum.explorewithme.main.models;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    /**
     * Широта географической точки.
     */
    @Column(name = "lat", nullable = false)
    private Float lat;

    /**
     * Долгота географической точки.
     */
    @Column(name = "lon", nullable = false)
    private Float lon;

}

