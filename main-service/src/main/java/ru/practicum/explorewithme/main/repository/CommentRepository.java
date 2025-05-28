package ru.practicum.explorewithme.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.main.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}