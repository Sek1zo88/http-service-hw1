package app.api;

import app.domain.Trainer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TrainerRequest(
        @NotBlank(message = "Имя тренера не может быть пустым")
        String name,

        @NotBlank(message = "Специализация тренера не может быть пустой")
        String specialty,

        @Positive(message = "Вместимость тренера должна быть больше 0")
        int maxCapacity) {

    public Trainer toDomain() {
        return new Trainer(null, name, specialty, maxCapacity);
    }
}
