package ru.practicum.explorewithme.main.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.service.CategoryService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCategoryController.class)
@DisplayName("Публичный контроллер категорий должен")
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryDto categoryDto;
    private CategoryDto anotherCategoryDto;

    @BeforeEach
    void setUp() {
        categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName("Тестовая категория");

        anotherCategoryDto = new CategoryDto();
        anotherCategoryDto.setId(2L);
        anotherCategoryDto.setName("Другая категория");
    }

    @Nested
    @DisplayName("при получении списка категорий")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("возвращать список всех категорий со статусом 200")
        void getAllCategories_ReturnsListOfCategories() throws Exception {
            List<CategoryDto> categories = Arrays.asList(categoryDto, anotherCategoryDto);

            when(categoryService.getAllCategories(anyInt(), anyInt())).thenReturn(categories);

            mockMvc.perform(get("/categories")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Тестовая категория")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Другая категория")));

            verify(categoryService, times(1)).getAllCategories(0, 10);
        }

        @Test
        @DisplayName("возвращать пустой список, если категорий нет")
        void getAllCategories_WhenNoCategories_ReturnsEmptyList() throws Exception {
            when(categoryService.getAllCategories(anyInt(), anyInt())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/categories")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(categoryService, times(1)).getAllCategories(0, 10);
        }

        @Test
        @DisplayName("применять параметры пагинации")
        void getAllCategories_WithPaginationParams_UsesThem() throws Exception {
            List<CategoryDto> categories = Collections.singletonList(categoryDto);

            when(categoryService.getAllCategories(eq(5), eq(3))).thenReturn(categories);

            mockMvc.perform(get("/categories")
                            .param("from", "5")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(categoryService, times(1)).getAllCategories(5, 3);
        }

        @Test
        @DisplayName("возвращать 400 при невалидных параметрах пагинации")
        void getAllCategories_WithInvalidPaginationParams_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/categories")
                            .param("from", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).getAllCategories(anyInt(), anyInt());
        }

        @Test
        @DisplayName("возвращать корректный заголовок Content-Type в ответе")
        void getAllCategories_ResponseHasCorrectContentTypeHeader() throws Exception {
            List<CategoryDto> categories = Collections.singletonList(categoryDto);
            when(categoryService.getAllCategories(anyInt(), anyInt())).thenReturn(categories);

            mockMvc.perform(get("/categories")
                            .param("from", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(categoryService, times(1)).getAllCategories(0, 10);
        }
    }

    @Nested
    @DisplayName("при получении категории по ID")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("возвращать категорию со статусом 200")
        void getCategoryById_ReturnsCategoryWithStatus200() throws Exception {
            when(categoryService.getCategoryById(eq(1L))).thenReturn(categoryDto);

            mockMvc.perform(get("/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Тестовая категория")));

            verify(categoryService, times(1)).getCategoryById(1L);
        }

        @Test
        @DisplayName("возвращать 404 при запросе несуществующей категории")
        void getCategoryById_WithNonExistingId_ReturnsNotFound() throws Exception {
            when(categoryService.getCategoryById(eq(999L)))
                    .thenThrow(new EntityNotFoundException("Category", "Id", 999L));

            mockMvc.perform(get("/categories/999"))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).getCategoryById(999L);
        }

        @Test
        @DisplayName("возвращать 400 при невалидном ID в пути")
        void getCategoryById_WithInvalidIdFormat_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/categories/invalid-id"))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).getCategoryById(anyLong());
        }

        @Test
        @DisplayName("возвращать корректный заголовок Content-Type в ответе")
        void getCategoryById_ResponseHasCorrectContentTypeHeader() throws Exception {
            when(categoryService.getCategoryById(anyLong())).thenReturn(categoryDto);

            mockMvc.perform(get("/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)));

            verify(categoryService, times(1)).getCategoryById(1L);
        }
    }

}