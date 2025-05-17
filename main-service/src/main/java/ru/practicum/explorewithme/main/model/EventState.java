package ru.practicum.explorewithme.main.model;

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

