package app.validation;

import app.domain.Client;
import app.domain.Trainer;

public final class GymValidator {
    private GymValidator() {
    }

    public static void validateClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Тело запроса не может быть пустым");
        }
        if (client.getName() == null || client.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя клиента не может быть пустым");
        }
        if (client.getType() == null) {
            throw new IllegalArgumentException("Тип абонемента должен быть BASIC или PREMIUM");
        }
    }

    public static void validateTrainer(Trainer trainer) {
        if (trainer == null) {
            throw new IllegalArgumentException("Тело запроса не может быть пустым");
        }
        if (trainer.getName() == null || trainer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя тренера не может быть пустым");
        }
        if (trainer.getSpecialty() == null || trainer.getSpecialty().trim().isEmpty()) {
            throw new IllegalArgumentException("Специализация тренера не может быть пустой");
        }
        if (trainer.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Вместимость тренера должна быть больше 0");
        }
    }
}
