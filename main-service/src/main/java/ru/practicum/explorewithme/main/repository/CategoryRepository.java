package ru.practicum.explorewithme.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.main.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}