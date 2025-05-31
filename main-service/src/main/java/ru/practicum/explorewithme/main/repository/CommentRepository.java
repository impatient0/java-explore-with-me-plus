package ru.practicum.explorewithme.main.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.main.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByEventIdAndIsDeletedFalse(Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByAuthorIdAndIsDeletedFalse(Long authorId, Pageable pageable);

}