package app.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.domain.Client;
import app.domain.Trainer;

public final class InMemoryGymRepository {
    private final Map<String, Trainer> trainers = new HashMap<>();
    private final Map<String, Client> clients = new HashMap<>();
    private final Map<String, Set<String>> bookings = new HashMap<>();

    public void saveTrainer(Trainer trainer) {
        trainers.put(trainer.getId(), trainer);
    }

    public Trainer findTrainer(String id) {
        return trainers.get(id);
    }

    public Collection<Trainer> findAllTrainers() {
        return trainers.values();
    }

    public Trainer removeTrainer(String id) {
        return trainers.remove(id);
    }

    public void saveClient(Client client) {
        clients.put(client.getId(), client);
        bookings.putIfAbsent(client.getId(), new HashSet<>());
    }

    public Client findClient(String id) {
        return clients.get(id);
    }

    public Collection<Client> findAllClients() {
        return clients.values();
    }

    public Client removeClient(String id) {
        return clients.remove(id);
    }

    public Set<String> bookingsForClient(String clientId) {
        return bookings.computeIfAbsent(clientId, ignored -> new HashSet<>());
    }

    public Set<String> removeBookingsForClient(String clientId) {
        return bookings.remove(clientId);
    }

    public List<String> removeTrainerFromBookings(String trainerId) {
        List<String> affectedClientIds = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : bookings.entrySet()) {
            if (entry.getValue().remove(trainerId)) {
                affectedClientIds.add(entry.getKey());
            }
        }
        return affectedClientIds;
    }
}
