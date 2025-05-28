package ru.practicum.explorewithme.main.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"author", "event"})
@EqualsAndHashCode(of = {"id"})
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    /**
     * Уникальный идентификатор комментария.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Текст комментария.
     */
    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    /**
     * Дата и время создания комментария. Устанавливается автоматически.
     */
    @CreatedDate
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    /**
     * Дата и время последнего обновления комментария. Устанавливается автоматически при изменении.
     */
    @LastModifiedDate
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    /**
     * Автор комментария.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User author;

    /**
     * Событие, к которому относится комментарий.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Event event;

    /**
     * Флаг, указывающий, был ли комментарий отредактирован.
     */
    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private boolean isEdited = false;

    /**
     * Флаг, указывающий, был ли комментарий удален.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}