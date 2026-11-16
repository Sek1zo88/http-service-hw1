package app.repository;

import app.domain.Client;
import app.domain.Trainer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GymRepository {
    void saveTrainer(Trainer trainer);

    Trainer findTrainer(String id);

    Collection<Trainer> findAllTrainers();

    Trainer removeTrainer(String id);

    void saveClient(Client client);

    Client findClient(String id);

    Collection<Client> findAllClients();

    Client removeClient(String id);

    Set<String> bookingsForClient(String clientId);

    Set<String> removeBookingsForClient(String clientId);

    List<String> removeTrainerFromBookings(String trainerId);
}
