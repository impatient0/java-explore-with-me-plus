package ru.practicum.explorewithme.main.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.dto.NewCategoryDto;
import ru.practicum.explorewithme.main.model.Category;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Маппер категорий должен")
class CategoryMapperTest {

    private final CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @Nested
    @DisplayName("при преобразовании Category в CategoryDto")
    class ToCategoryDtoTests {

        @Test
        @DisplayName("корректно маппить все поля")
        void toDto_ShouldMapAllFields() {

            Category category = new Category();
            category.setId(1L);
            category.setName("Тестовая категория");

            CategoryDto result = categoryMapper.toDto(category);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(category.getId());
            assertThat(result.getName()).isEqualTo(category.getName());
        }

        @Test
        @DisplayName("возвращать null при преобразовании null")
        void toDto_ShouldReturnNullWhenCategoryIsNull() {

            CategoryDto result = categoryMapper.toDto(null);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("при преобразовании NewCategoryDto в Category")
    class ToCategoryTests {

        @Test
        @DisplayName("корректно маппить все поля")
        void toCategory_ShouldMapAllFields() {

            NewCategoryDto newCategoryDto = new NewCategoryDto();
            newCategoryDto.setName("Новая категория");

            Category result = categoryMapper.toCategory(newCategoryDto);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(newCategoryDto.getName());
            // Id должен игнорироваться маппером согласно аннотации @Mapping(target = "id", ignore = true)
            assertThat(result.getId()).isNull();
        }

        @Test
        @DisplayName("возвращать null при преобразовании null")
        void toCategory_ShouldReturnNullWhenNewCategoryDtoIsNull() {

            Category result = categoryMapper.toCategory(null);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("при сквозных тестах маппинга")
    class IntegrationTests {

        @Test
        @DisplayName("сохранять все поля при цепочке преобразований")
        void mapper_ShouldPreserveAllFieldsInConversionChain() {

            NewCategoryDto newCategoryDto = new NewCategoryDto();
            newCategoryDto.setName("Тестовая категория");

            Category category = categoryMapper.toCategory(newCategoryDto);
            category.setId(1L);

            CategoryDto categoryDto = categoryMapper.toDto(category);

            // Проверка полного цикла преобразования
            assertThat(categoryDto.getId()).isEqualTo(category.getId());
            assertThat(categoryDto.getName()).isEqualTo(newCategoryDto.getName());
        }
    }

    @Nested
    @DisplayName("при работе с граничными случаями")
    class EdgeCasesTests {

        @Test
        @DisplayName("корректно обрабатывать пустые строки")
        void mapper_ShouldHandleEmptyStrings() {

            NewCategoryDto newCategoryDto = new NewCategoryDto();
            newCategoryDto.setName("");

            Category category = categoryMapper.toCategory(newCategoryDto);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEmpty();
        }

        @Test
        @DisplayName("корректно обрабатывать специальные символы")
        void mapper_ShouldHandleSpecialCharacters() {

            String specialName = "Категория с !@#$%^&*()_+";

            NewCategoryDto newCategoryDto = new NewCategoryDto();
            newCategoryDto.setName(specialName);

            Category category = categoryMapper.toCategory(newCategoryDto);
            CategoryDto categoryDto = categoryMapper.toDto(category);

            assertThat(categoryDto.getName()).isEqualTo(specialName);
        }
    }
}