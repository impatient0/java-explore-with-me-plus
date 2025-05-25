package ru.practicum.explorewithme.main.controller.pub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.CompilationDto;
import ru.practicum.explorewithme.main.service.CompilationService;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCompilationController {

    private final CompilationService compilationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CompilationDto> getCompilations(
            @RequestParam(name = "pinned", required = false) Boolean pinned,
            @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(name = "size", defaultValue = "10") @Positive int size) {
        log.info("Received request to get compilations with pinned={}, from={}, size={}", pinned, from, size);
        List<CompilationDto> result = compilationService.getCompilations(pinned, from, size);
        log.info("Found {} compilations", result.size());
        return result;
    }

    @GetMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto getCompilationById(@PathVariable @Positive Long compId) {
        log.info("Received request to get compilation with id={}", compId);
        CompilationDto result = compilationService.getCompilationById(compId);
        log.info("Found compilation: {}", result);
        return result;
    }
}