package app.api;

import app.domain.Trainer;

public record TrainerResponse(
        String id,
        String name,
        String specialty,
        int activeBookings,
        int maxCapacity) {

    public static TrainerResponse from(Trainer trainer) {
        return new TrainerResponse(
                trainer.getId(),
                trainer.getName(),
                trainer.getSpecialty(),
                trainer.getActiveBookings(),
                trainer.getMaxCapacity());
    }
}
