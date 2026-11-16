package app.api;

import app.domain.Client;
import app.domain.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotBlank(message = "Имя клиента не может быть пустым")
        String name,

        @NotNull(message = "Тип абонемента должен быть BASIC или PREMIUM")
        ClientType type) {

    public Client toDomain() {
        return new Client(null, name, type);
    }
}
