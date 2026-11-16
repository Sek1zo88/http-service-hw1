package app.api;

import app.domain.Client;
import app.domain.ClientType;

public record ClientResponse(
        String id,
        String name,
        ClientType type,
        int activeBookings) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getType(),
                client.getActiveBookings());
    }
}
