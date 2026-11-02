package app.service;

import app.domain.Client;
import app.domain.ClientType;
import app.domain.Trainer;
import app.exception.BusinessRuleException;
import app.exception.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GymService {
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Trainer> trainers = new ConcurrentHashMap<>();

    public Trainer createTrainer(Trainer trainer) {
        String id = UUID.randomUUID().toString();
        trainer.setId(id);
        trainer.setActiveBookings(0);
        trainers.put(id, trainer);
        return trainer;
    }

    public Trainer getTrainer(String id) {
        if (!trainers.containsKey(id)) throw new NotFoundException("Тренер не найден");
        return trainers.get(id);
    }

    public List<Trainer> getTrainers(String specialtyFilter) {
        return trainers.values().stream()
                .filter(t -> specialtyFilter == null || t.getSpecialty().equalsIgnoreCase(specialtyFilter))
                .collect(Collectors.toList());
    }

    public Trainer updateTrainer(String id, Trainer updated) {
        Trainer existing = getTrainer(id);
        existing.setName(updated.getName());
        existing.setSpecialty(updated.getSpecialty());
        existing.setMaxCapacity(updated.getMaxCapacity());
        return existing;
    }

    public void deleteTrainer(String id) {
        if (trainers.remove(id) == null) throw new NotFoundException("Тренер не найден");
    }

    public Client createClient(Client client) {
        String id = UUID.randomUUID().toString();
        client.setId(id);
        client.setActiveBookings(0);
        clients.put(id, client);
        return client;
    }

    public Client getClient(String id) {
        if (!clients.containsKey(id)) throw new NotFoundException("Клиент не найден");
        return clients.get(id);
    }

    public List<Client> getClients(ClientType typeFilter) {
        return clients.values().stream()
                .filter(c -> typeFilter == null || c.getType() == typeFilter)
                .collect(Collectors.toList());
    }

    public Client updateClient(String id, Client updated) {
        Client existing = getClient(id);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        return existing;
    }

    public void deleteClient(String id) {
        if (clients.remove(id) == null) throw new NotFoundException("Клиент не найден");
    }

    public void bookSession(String clientId, String trainerId) {
        Client client = getClient(clientId);
        Trainer trainer = getTrainer(trainerId);

        if (trainer.getActiveBookings() >= trainer.getMaxCapacity()) {
            throw new BusinessRuleException("У тренера нет свободных мест");
        }

        if (client.getType() == ClientType.BASIC && client.getActiveBookings() >= 1) {
            throw new BusinessRuleException("Базовый абонемент позволяет иметь только 1 активную запись");
        }
        
        if (client.getType() == ClientType.PREMIUM && client.getActiveBookings() >= 5) {
            throw new BusinessRuleException("Премиум абонемент позволяет иметь не более 5 активных записей");
        }

        client.setActiveBookings(client.getActiveBookings() + 1);
        trainer.setActiveBookings(trainer.getActiveBookings() + 1);
    }
}