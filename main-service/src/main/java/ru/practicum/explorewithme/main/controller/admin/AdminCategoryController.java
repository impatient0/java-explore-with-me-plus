package ru.practicum.explorewithme.main.controller.admin;

import ru.practicum.explorewithme.main.dto.NewCategoryDto;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("Admin: Received request to add category: {}", newCategoryDto);
        CategoryDto result = categoryService.createCategory(newCategoryDto);
        log.info("Admin: Adding category: {}", result);
        return result;
    }

    @PatchMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto updateCategory(@PathVariable Long categoryId,
                                      @Valid @RequestBody NewCategoryDto categoryDto) {
        log.info("Admin: Received request to update category with Id: {}, new data: {}", categoryId, categoryDto);
        CategoryDto result = categoryService.updateCategory(categoryId, categoryDto);
        log.info("Admin: Updated category: {}", result);
        return result;
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        log.info("Admin: Received request to delete category with Id: {}", categoryId);
        categoryService.deleteCategory(categoryId);
        log.info("Admin: Delete category with Id: {}", categoryId);
    }

}