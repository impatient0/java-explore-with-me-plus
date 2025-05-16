package ru.practicum.explorewithme.main.mapper; // Пример пакета

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.models.Category;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.models.Event;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.models.User;

import java.util.List;

// componentModel = "spring" позволит внедрять маппер как бин
@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    // Если CategoryMapper и UserMapper определены и используются (см. ниже)
    @Mapping(source = "category", target = "category") // MapStruct сам вызовет CategoryMapper.toDto(Category)
    @Mapping(source = "initiator", target = "initiator") // MapStruct сам вызовет UserMapper.toShortDto(User)
    @Mapping(target = "confirmedRequests", expression = "java(0L)") // Временная заглушка
    @Mapping(target = "views", expression = "java(0L)") // Временная заглушка
    EventFullDto toEventFullDto(Event event);

    List<EventFullDto> toEventFullDtoList(List<Event> events);

    // Если вы хотите мапить связанные сущности прямо здесь:
    // @Mapping(source = "event.category", target = "category", qualifiedByName = "categoryToCategoryDto")
    // @Mapping(source = "event.initiator", target = "initiator", qualifiedByName = "userToUserShortDto")
    // EventFullDto toEventFullDtoManualSubMapping(Event event);

    // @Named("categoryToCategoryDto")
    // default CategoryDto categoryToCategoryDto(Category category) {
    //     if (category == null) return null;
    //     return CategoryDto.builder().id(category.getId()).name(category.getName()).build();
    // }

    // @Named("userToUserShortDto")
    // default UserShortDto userToUserShortDto(User user) {
    //     if (user == null) return null;
    //     return UserShortDto.builder().id(user.getId()).name(user.getName()).build();
    // }
}

// Отдельный маппер для Category (если он используется во многих местах)
// Либо можно было бы использовать default методы или @Named методы в EventMapper, как закомментировано выше.
@Mapper(componentModel = "spring")
interface CategoryMapper {
    CategoryDto toDto(Category category);
    // Category toEntity(CategoryDto categoryDto); // Если нужен обратный маппинг
}

// Отдельный маппер для User
@Mapper(componentModel = "spring")
interface UserMapper {
    UserShortDto toShortDto(User user);
    // User toEntity(UserShortDto userShortDto); // Если нужен обратный маппинг
}