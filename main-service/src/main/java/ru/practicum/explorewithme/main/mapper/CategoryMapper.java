package ru.practicum.explorewithme.main.mapper;

import org.mapstruct.Mapper;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.models.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category category);
}