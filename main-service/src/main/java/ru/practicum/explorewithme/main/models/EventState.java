package ru.practicum.explorewithme.main.models;

/**
 * Состояния жизненного цикла события
 */
public enum EventState {
    /**
     * Ожидает модерации
     */
    PENDING,

    /**
     * Опубликовано
     */
    PUBLISHED,

    /**
     * Отменено
     */
    CANCELED
}

