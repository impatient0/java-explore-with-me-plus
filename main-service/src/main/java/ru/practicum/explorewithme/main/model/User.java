package ru.practicum.explorewithme.main.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id", "email"})
public class User {

    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Имя пользователя.
     */
    @Column(name = "name", nullable = false, length = 250)
    private String name;

    /**
     * Электронная почта пользователя (уникальный идентификатор).
     */
    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

}
