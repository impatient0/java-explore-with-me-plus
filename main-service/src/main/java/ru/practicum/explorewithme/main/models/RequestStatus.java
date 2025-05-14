package ru.practicum.explorewithme.main.models;

/**
 * Статусы запросов на участие в событии
 */
public enum RequestStatus {
    /**
     * Ожидает подтверждения
     */
    PENDING,

    /**
     * Подтвержден
     */
    CONFIRMED,

    /**
     * Отклонен
     */
    REJECTED,

    /**
     * Отменен
     */
    CANCELED
}

