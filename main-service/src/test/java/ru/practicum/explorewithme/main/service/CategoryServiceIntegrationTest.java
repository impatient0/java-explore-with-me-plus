package ru.practicum.explorewithme.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.dto.NewCategoryDto;
import ru.practicum.explorewithme.main.dto.NewUserRequestDto;
import ru.practicum.explorewithme.main.error.EntityAlreadyExistsException;
import ru.practicum.explorewithme.main.error.EntityDeletedException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.UserMapper;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.practicum.explorewithme.main.repository.EventRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Интеграционное тестирование CategoryServiceImpl")
class CategoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16.1")
            .withDatabaseName("explorewithme_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    private NewCategoryDto newCategoryDto;
    private NewCategoryDto anotherCategoryDto;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Тестовая категория");

        anotherCategoryDto = new NewCategoryDto();
        anotherCategoryDto.setName("Другая категория");
    }

    @Nested
    @DisplayName("Создание категории")
    class CreateCategoryTests {

        @Test
        @DisplayName("Успешное создание категории")
        void createCategory_Success() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

            assertNotNull(createdCategory);
            assertNotNull(createdCategory.getId());
            assertEquals(newCategoryDto.getName(), createdCategory.getName());

            Optional<Category> categoryFromDb = categoryRepository.findById(createdCategory.getId());
            assertTrue(categoryFromDb.isPresent());
            assertEquals(newCategoryDto.getName(), categoryFromDb.get().getName());
        }

        @Test
        @DisplayName("Исключение при создании категории с уже существующим именем")
        void createCategory_WithExistingName_ThrowsException() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);
            assertNotNull(createdCategory);

            NewCategoryDto duplicateCategoryDto = new NewCategoryDto();
            duplicateCategoryDto.setName(newCategoryDto.getName());

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
                categoryService.createCategory(duplicateCategoryDto);
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(duplicateCategoryDto.getName()));
        }

    }

    @Nested
    @DisplayName("Обновление категории")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Успешное обновление категории")
        void updateCategory_Success() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

            NewCategoryDto updateDto = new NewCategoryDto();
            updateDto.setName("Обновленная категория");

            CategoryDto updatedCategory = categoryService.updateCategory(createdCategory.getId(), updateDto);

            assertNotNull(updatedCategory);
            assertEquals(createdCategory.getId(), updatedCategory.getId());
            assertEquals(updateDto.getName(), updatedCategory.getName());

            Optional<Category> categoryFromDb = categoryRepository.findById(createdCategory.getId());
            assertTrue(categoryFromDb.isPresent());
            assertEquals(updateDto.getName(), categoryFromDb.get().getName());
        }

        @Test
        @DisplayName("Исключение при обновлении несуществующей категории")
        void updateCategory_CategoryNotFound_ThrowsException() {

            Long nonExistentCategoryId = 999L;

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                categoryService.updateCategory(nonExistentCategoryId, newCategoryDto);
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(nonExistentCategoryId.toString()));
        }

        @Test
        @DisplayName("Обновление категории с пустым именем не меняет существующее значение")
        void updateCategory_WithBlankName_PreservesExistingName() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

            NewCategoryDto updateDto = new NewCategoryDto();
            updateDto.setName(""); // Пустое имя

            CategoryDto updatedCategory = categoryService.updateCategory(createdCategory.getId(), updateDto);

            assertEquals(newCategoryDto.getName(), updatedCategory.getName());
        }

        @Test
        @DisplayName("Обновление категории с тем же самым именем не вызывает ошибок")
        void updateCategory_WithSameName_Success() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

            NewCategoryDto updateDto = new NewCategoryDto();
            updateDto.setName(createdCategory.getName());

            CategoryDto updatedCategory = categoryService.updateCategory(createdCategory.getId(), updateDto);

            assertNotNull(updatedCategory);
            assertEquals(createdCategory.getId(), updatedCategory.getId());
            assertEquals(createdCategory.getName(), updatedCategory.getName());

            Optional<Category> categoryFromDb = categoryRepository.findById(createdCategory.getId());
            assertTrue(categoryFromDb.isPresent());
            assertEquals(createdCategory.getName(), categoryFromDb.get().getName());
        }

        @Test
        @DisplayName("Исключение при обновлении категории с именем, которое уже существует")
        void updateCategory_WithExistingName_ThrowsException() {

            CategoryDto firstCategory = categoryService.createCategory(newCategoryDto);
            CategoryDto secondCategory = categoryService.createCategory(anotherCategoryDto);

            NewCategoryDto updateDto = new NewCategoryDto();
            updateDto.setName(secondCategory.getName());

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
                categoryService.updateCategory(firstCategory.getId(), updateDto);
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(updateDto.getName()));
        }

    }

    @Nested
    @DisplayName("Удаление категории")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Успешное удаление категории")
        void deleteCategory_Success() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

            assertTrue(categoryRepository.existsById(createdCategory.getId()));

            categoryService.deleteCategory(createdCategory.getId());

            assertFalse(categoryRepository.existsById(createdCategory.getId()));
        }

        @Test
        @DisplayName("Исключение при удалении несуществующей категории")
        void deleteCategory_CategoryNotFound_ThrowsException() {

            Long nonExistentCategoryId = 999L;

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                categoryService.deleteCategory(nonExistentCategoryId);
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(nonExistentCategoryId.toString()));
        }

        @Test
        @DisplayName("Исключение при удалении категории, содержащей события")
        void deleteCategory_WithEvents_ThrowsException() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);
            assertNotNull(createdCategory);

            User user = userMapper.toUser(userService
                    .createUser(new NewUserRequestDto("Test name", "Test email")));

            Event event = Event.builder()
                    .annotation("Test annotation")
                    .createdOn(java.time.LocalDateTime.now())
                    .category(new Category(createdCategory.getId(), createdCategory.getName()))
                    .description("Test description")
                    .eventDate(java.time.LocalDateTime.now())
                    .initiator(user)
                    .location(new Location(5555.55F, 5555.555F))
                    .title("Test title")
                    .publishedOn(java.time.LocalDateTime.now())
                    .state(EventState.PENDING)
                    .build();

            eventRepository.save(event);

            EntityDeletedException exception = assertThrows(EntityDeletedException.class, () -> {
                categoryService.deleteCategory(createdCategory.getId());
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(createdCategory.getId().toString()));
        }

    }

    @Nested
    @DisplayName("Получение списка категорий")
    class GetCategoriesTests {

        @Test
        @DisplayName("Получение всех категорий")
        void getAllCategories_ReturnsAllCategories() {

            CategoryDto category1 = categoryService.createCategory(newCategoryDto);
            CategoryDto category2 = categoryService.createCategory(anotherCategoryDto);

            List<CategoryDto> categories = categoryService.getAllCategories(0, 10);

            assertNotNull(categories);
            assertEquals(2, categories.size());

            List<Long> categoryIds = categories.stream()
                    .map(CategoryDto::getId)
                    .collect(Collectors.toList());
            assertTrue(categoryIds.contains(category1.getId()));
            assertTrue(categoryIds.contains(category2.getId()));
        }

        @Test
        @DisplayName("Корректная работа пагинации при получении категорий")
        void getAllCategories_Pagination_ReturnsCorrectPage() {

            List<CategoryDto> createdCategories = IntStream.range(0, 5)
                    .mapToObj(i -> {
                        NewCategoryDto request = new NewCategoryDto();
                        request.setName("Category " + i);
                        return categoryService.createCategory(request);
                    })
                    .collect(Collectors.toList());

            List<CategoryDto> page1 = categoryService.getAllCategories(0, 2);

            List<CategoryDto> page2 = categoryService.getAllCategories(2, 2);

            List<CategoryDto> page3 = categoryService.getAllCategories(4, 2);

            assertEquals(2, page1.size());
            assertEquals(2, page2.size());
            assertEquals(1, page3.size());

            List<Long> allCategoryIds = new java.util.ArrayList<>();
            allCategoryIds.addAll(page1.stream().map(CategoryDto::getId).collect(Collectors.toList()));
            allCategoryIds.addAll(page2.stream().map(CategoryDto::getId).collect(Collectors.toList()));
            allCategoryIds.addAll(page3.stream().map(CategoryDto::getId).collect(Collectors.toList()));

            assertEquals(5, allCategoryIds.size());
            assertEquals(5, allCategoryIds.stream().distinct().count());
        }

        @Test
        @DisplayName("Получение пустого списка при отсутствии категорий")
        void getAllCategories_EmptyRepository_ReturnsEmptyList() {

            List<CategoryDto> categories = categoryService.getAllCategories(0, 10);

            assertNotNull(categories);
            assertTrue(categories.isEmpty());
        }

        @Test
        @DisplayName("Исключение при создании категории с уже существующим именем")
        void createCategory_WithExistingName_ThrowsException() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);
            assertNotNull(createdCategory);

            NewCategoryDto duplicateCategoryDto = new NewCategoryDto();
            duplicateCategoryDto.setName(newCategoryDto.getName());

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class, () -> {
                categoryService.createCategory(duplicateCategoryDto);
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(duplicateCategoryDto.getName()));
        }
    }

    @Nested
    @DisplayName("Получение категории по ID")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Успешное получение категории по ID")
        void getCategoryById_Success() {

            CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

            CategoryDto retrievedCategory = categoryService.getCategoryById(createdCategory.getId());

            assertNotNull(retrievedCategory);
            assertEquals(createdCategory.getId(), retrievedCategory.getId());
            assertEquals(createdCategory.getName(), retrievedCategory.getName());
        }

        @Test
        @DisplayName("Исключение при запросе несуществующей категории")
        void getCategoryById_CategoryNotFound_ThrowsException() {

            Long nonExistentCategoryId = 999L;

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                categoryService.getCategoryById(nonExistentCategoryId);
            });

            assertTrue(exception.getMessage().contains("Category"));
            assertTrue(exception.getMessage().contains(nonExistentCategoryId.toString()));
        }
    }

    @Nested
    @DisplayName("Тесты производительности")
    class PerformanceTests {

        @Test
        @DisplayName("Эффективная работа с большим количеством данных")
        void getAllCategories_WithLargeDataset_PerformsEfficiently() {

            for (int i = 0; i < 100; i++) {
                NewCategoryDto request = new NewCategoryDto();
                request.setName("Category " + i);
                categoryService.createCategory(request);
            }

            long startTime = System.currentTimeMillis();
            List<CategoryDto> categories = categoryService.getAllCategories(0, 50);
            long endTime = System.currentTimeMillis();

            assertEquals(50, categories.size());
            assertTrue((endTime - startTime) < 1000); // Ожидаем выполнение менее чем за секунду

            System.out.println("Время выполнения запроса для 50 категорий из 100: " + (endTime - startTime) + " мс");
        }
    }

    @Nested
    @DisplayName("Тесты обработки граничных случаев")
    class EdgeCaseTests {

        @Test
        @DisplayName("Корректная обработка запроса страницы за пределами допустимого диапазона")
        void getAllCategories_PageOutOfRange_ReturnsEmptyList() {

            IntStream.range(0, 3)
                    .forEach(i -> {
                        NewCategoryDto request = new NewCategoryDto();
                        request.setName("Category " + i);
                        categoryService.createCategory(request);
                    });

            List<CategoryDto> result = categoryService.getAllCategories(10, 5);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}