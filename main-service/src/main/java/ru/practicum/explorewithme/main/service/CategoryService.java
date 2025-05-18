package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long categoryId);

    CategoryDto updateCategory(Long categoryId, NewCategoryDto categoryDto);

    CategoryDto getCategoryById(Long categoryId);

    List<CategoryDto> getAllCategories(int from, int size);
}
