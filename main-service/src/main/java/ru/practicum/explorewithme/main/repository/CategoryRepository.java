package ru.practicum.explorewithme.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.main.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END" +
            " FROM Category c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))")
    boolean existsByNameIgnoreCaseAndTrim(@Param("name") String name);

}