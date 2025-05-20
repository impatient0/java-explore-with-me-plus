package ru.practicum.explorewithme.main.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.explorewithme.common.error.ApiError;

@RestControllerAdvice
@Slf4j
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        String errorMessage = "Validation error(s): " + String.join("; ", errors);
        log.warn(errorMessage, e);
        return ApiError.builder()
            .errors(errors)
            .status(HttpStatus.BAD_REQUEST)
            .reason("Incorrectly made request due to validation errors.")
            .message(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameter(final MissingServletRequestParameterException e) {
        String errorMessage = "Required request parameter is not present: " + e.getParameterName();
        log.warn(errorMessage, e);
        return ApiError.builder()
            .status(HttpStatus.BAD_REQUEST)
            .reason("Incorrectly made request.")
            .message(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(final IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage(), e);
        return ApiError.builder()
            .status(HttpStatus.BAD_REQUEST)
            .reason("Incorrectly made request due to an invalid argument.")
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.warn("Database integrity violation: {}", e.getMessage(), e);
        return ApiError.builder()
            .status(HttpStatus.CONFLICT)
            .reason("Integrity constraint has been violated.")
            .message("A database integrity constraint was violated: " + e.getMostSpecificCause().getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ApiError.builder()
            .status(HttpStatus.BAD_REQUEST)
            .reason("Malformed JSON request.")
            .message("The request body is malformed or unreadable: " + e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolationException(final ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations()
            .stream()
            .map(violation -> String.format("Parameter '%s': value '%s' %s",
                extractParameterName(violation),
                violation.getInvalidValue(),
                violation.getMessage()))
            .collect(Collectors.toList());

        String errorMessage = "Validation constraint(s) violated: " + String.join("; ", errors);
        log.warn(errorMessage, e);

        return ApiError.builder()
            .errors(errors)
            .status(HttpStatus.BAD_REQUEST)
            .reason("One or more validation constraints were violated.")
            .message(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        String parameterName = e.getName();
        Object invalidValue = e.getValue();
        Class<?> requiredType = e.getRequiredType(); // Ожидаемый тип

        String message;
        if (requiredType != null) {
            message = String.format("Parameter '%s' should be of type '%s' but was '%s'.",
                parameterName, requiredType.getSimpleName(), invalidValue);
        } else {
            message = String.format("Parameter '%s' has an invalid value '%s'.",
                parameterName, invalidValue);
        }

        log.warn("Type mismatch for parameter '{}': required type '{}', value '{}'. Full exception: {}",
            parameterName, requiredType != null ? requiredType.getName() : "unknown", invalidValue, e.getMessage());

        return ApiError.builder()
            .status(HttpStatus.BAD_REQUEST)
            .reason("Incorrectly made request due to a type mismatch for a request parameter.")
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEntityAlreadyExistsException(EntityAlreadyExistsException e) {
        log.warn("Entity already exist: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.CONFLICT)
                .reason("Requested object already exists")
                .message("Requested object already exists" + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.NOT_FOUND)
                .reason("Requested object not found")
                .message("Requested object not found" + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(EntityDeletedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEntityDeletedException(EntityDeletedException e) {
        log.warn("Entity restriction of removal - not empty");
        return ApiError.builder()
                .status(HttpStatus.NOT_FOUND)
                .reason("Restriction of removal")
                .message("Restriction of removal" + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable e) {
        log.error("An unexpected error occurred: {}", e.getMessage(), e);
        return ApiError.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .reason("An unexpected error occurred on the server.")
            .message("An internal server error has occurred: " + e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }

    private String extractParameterName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        if (propertyPath.contains(".")) {
            return propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
        }
        return propertyPath;
    }
}

