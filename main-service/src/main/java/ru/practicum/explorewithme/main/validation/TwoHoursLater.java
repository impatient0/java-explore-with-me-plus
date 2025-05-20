package ru.practicum.explorewithme.main.validation; // Пример пакета

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TwoHoursLaterValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TwoHoursLater {
    String message() default "Event date must be at least two hours in the future from the current moment";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}