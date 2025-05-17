package ru.practicum.explorewithme.main.error;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, String fieldName, Object value) {
        super(String.format("%s with %s = '%s' not found", entityName, fieldName, value));
    }
}