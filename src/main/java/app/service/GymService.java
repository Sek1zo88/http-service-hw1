package app.service;

import app.domain.Client;
import app.domain.Trainer;
import app.exception.BusinessRuleException;
import app.exception.NotFoundException;
import app.repository.InMemoryGymRepository;
import app.validation.GymValidator;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GymService {
    private final InMemoryGymRepository repository = new InMemoryGymRepository();

    public synchronized Trainer createTrainer(Trainer trainer) {
        GymValidator.validateTrainer(trainer);

        String id = UUID.randomUUID().toString();
        trainer.setId(id);
        trainer.setActiveBookings(0);

        repository.saveTrainer(copyTrainer(trainer));
        return copyTrainer(trainer);
    }

    public synchronized Trainer getTrainer(String id) {
        return copyTrainer(findTrainer(id));
    }

    public synchronized List<Trainer> getTrainers(String specialty) {
        return repository.findAllTrainers().stream()
                .filter(trainer -> specialty == null
                        || specialty.equalsIgnoreCase(trainer.getSpecialty()))
                .map(GymService::copyTrainer)
                .collect(Collectors.toList());
    }

    public synchronized Trainer updateTrainer(String id, Trainer updated) {
        GymValidator.validateTrainer(updated);
        Trainer existing = findTrainer(id);

        if (updated.getMaxCapacity() < existing.getActiveBookings()) {
            throw new BusinessRuleException("Новая вместимость (" + updated.getMaxCapacity()
                    + ") не может быть меньше текущего числа записей ("
                    + existing.getActiveBookings() + ")");
        }

        existing.setName(updated.getName());
        existing.setSpecialty(updated.getSpecialty());
        existing.setMaxCapacity(updated.getMaxCapacity());
        return copyTrainer(existing);
    }

    public synchronized void deleteTrainer(String id) {
        if (repository.removeTrainer(id) == null) {
            throw new NotFoundException("Тренер не найден");
        }

        for (String clientId : repository.removeTrainerFromBookings(id)) {
            Client client = repository.findClient(clientId);
            if (client != null) {
                client.setActiveBookings(client.getActiveBookings() - 1);
            }
        }
    }

    public synchronized Client createClient(Client client) {
        GymValidator.validateClient(client);

        String id = UUID.randomUUID().toString();
        client.setId(id);
        client.setActiveBookings(0);

        repository.saveClient(copyClient(client));
        return copyClient(client);
    }

    public synchronized Client getClient(String id) {
        return copyClient(findClient(id));
    }

    public synchronized List<Client> getClients(String type) {
        return repository.findAllClients().stream()
                .filter(client -> type == null
                        || type.equalsIgnoreCase(client.getType().name()))
                .map(GymService::copyClient)
                .collect(Collectors.toList());
    }

    public synchronized Client updateClient(String id, Client updated) {
        GymValidator.validateClient(updated);
        Client existing = findClient(id);

        int newLimit = updated.getType().getLimit();
        if (existing.getActiveBookings() > newLimit) {
            throw new BusinessRuleException("Смена абонемента на " + updated.getType()
                    + " невозможна: у клиента уже " + existing.getActiveBookings()
                    + " записей (лимит " + newLimit + ")");
        }

        existing.setName(updated.getName());
        existing.setType(updated.getType());
        return copyClient(existing);
    }

    public synchronized void deleteClient(String id) {
        if (repository.removeClient(id) == null) {
            throw new NotFoundException("Клиент не найден");
        }

        Set<String> clientBookings = repository.removeBookingsForClient(id);
        if (clientBookings != null) {
            for (String trainerId : clientBookings) {
                Trainer trainer = repository.findTrainer(trainerId);
                if (trainer != null) {
                    trainer.setActiveBookings(trainer.getActiveBookings() - 1);
                }
            }
        }
    }

    public synchronized void bookSession(String clientId, String trainerId) {
        Client client = findClient(clientId);
        Trainer trainer = findTrainer(trainerId);
        Set<String> clientBookings = repository.bookingsForClient(clientId);

        if (clientBookings.contains(trainerId)) {
            throw new BusinessRuleException("Клиент уже записан к этому тренеру");
        }
        if (trainer.getActiveBookings() >= trainer.getMaxCapacity()) {
            throw new BusinessRuleException("У тренера нет свободных мест");
        }
        if (client.getActiveBookings() >= client.getType().getLimit()) {
            throw new BusinessRuleException(
                    "Исчерпан лимит записей по абонементу " + client.getType());
        }

        clientBookings.add(trainerId);
        client.setActiveBookings(client.getActiveBookings() + 1);
        trainer.setActiveBookings(trainer.getActiveBookings() + 1);
    }

    private Trainer findTrainer(String id) {
        Trainer trainer = repository.findTrainer(id);
        if (trainer == null) {
            throw new NotFoundException("Тренер не найден");
        }
        return trainer;
    }

    private Client findClient(String id) {
        Client client = repository.findClient(id);
        if (client == null) {
            throw new NotFoundException("Клиент не найден");
        }
        return client;
    }

    private static Trainer copyTrainer(Trainer trainer) {
        Trainer copy = new Trainer(
                trainer.getId(),
                trainer.getName(),
                trainer.getSpecialty(),
                trainer.getMaxCapacity());
        copy.setActiveBookings(trainer.getActiveBookings());
        return copy;
    }

    private static Client copyClient(Client client) {
        Client copy = new Client(client.getId(), client.getName(), client.getType());
        copy.setActiveBookings(client.getActiveBookings());
        return copy;
    }
}
