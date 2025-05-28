package ru.practicum.explorewithme.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.model.Comment;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {

    /**
     * Маппинг из NewCommentDto в сущность Comment.
     * Поля author и event должны быть установлены в сервисе отдельно.
     * Поля id, createdOn, updatedOn, isEdited, isDeleted будут установлены автоматически/в логике.
     */
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "createdOn", ignore = true),
        @Mapping(target = "updatedOn", ignore = true),
        @Mapping(target = "author", ignore = true),
        @Mapping(target = "event", ignore = true),
        @Mapping(target = "isEdited", ignore = true),
        @Mapping(target = "isDeleted", ignore = true)
    })
    Comment toComment(NewCommentDto newCommentDto);


    /**
     * Маппинг из сущности Comment в CommentDto.
     * Поле eventId извлекается из comment.getEvent().getId().
     */
    @Mappings({
        @Mapping(source = "event.id", target = "eventId"),
        @Mapping(source = "edited", target = "isEdited")
    })
    CommentDto toDto(Comment comment);

    List<CommentDto> toDtoList(List<Comment> comments);

}