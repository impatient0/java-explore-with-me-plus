package ru.practicum.explorewithme.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.practicum.explorewithme.main.dto.CompilationDto;
import ru.practicum.explorewithme.main.dto.NewCompilationDto;
import ru.practicum.explorewithme.main.dto.UpdateCompilationRequestDto;
import ru.practicum.explorewithme.main.model.Compilation;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(target = "events", source = "events")
    CompilationDto toDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateCompilationFromDto(UpdateCompilationRequestDto dto, @MappingTarget Compilation compilation);
}