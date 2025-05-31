package ru.practicum.explorewithme.main.repository;


import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.explorewithme.main.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long>,
    QuerydslPredicateExecutor<Comment> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByEventIdAndIsDeletedFalse(Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Override
    @NotNull Page<Comment> findAll(@NotNull Predicate predicate, @NotNull Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByAuthorIdAndIsDeletedFalse(Long authorId, Pageable pageable);

}