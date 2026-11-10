package app.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import app.domain.Client;
import app.domain.Trainer;
import app.exception.BusinessRuleException;
import app.exception.NotFoundException;

public class GymService {
    private final Map<String, Trainer> trainers = new ConcurrentHashMap<>();
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    
    private final Map<String, Set<String>> bookings = new ConcurrentHashMap<>();

    public synchronized Trainer createTrainer(Trainer trainer) {
        validateTrainer(trainer);
        String id = UUID.randomUUID().toString();
        trainer.setId(id);
        trainer.setActiveBookings(0);
        trainers.put(id, trainer);
        return trainer;
    }

    public Trainer getTrainer(String id) {
        Trainer trainer = trainers.get(id);
        if (trainer == null) throw new NotFoundException("Тренер не найден");
        return trainer;
    }

    public List<Trainer> getTrainers(String specialty) {
        return trainers.values().stream()
                .filter(t -> specialty == null || specialty.equals(t.getSpecialty()))
                .collect(Collectors.toList());
    }

    public synchronized Trainer updateTrainer(String id, Trainer updated) {
        validateTrainer(updated);
        Trainer existing = getTrainer(id);
        
        if (updated.getMaxCapacity() < existing.getActiveBookings()) {
            throw new BusinessRuleException("Новая вместимость (" + updated.getMaxCapacity() + 
                ") не может быть меньше текущего числа записей (" + existing.getActiveBookings() + ")");
        }
        
        existing.setName(updated.getName());
        existing.setSpecialty(updated.getSpecialty());
        existing.setMaxCapacity(updated.getMaxCapacity());
        return existing;
    }

    public synchronized void deleteTrainer(String id) {
        if (trainers.remove(id) == null) {
            throw new NotFoundException("Тренер не найден");
        }
        bookings.values().forEach(clientBookings -> clientBookings.remove(id));
    }

    public synchronized Client createClient(Client client) {
        validateClient(client);
        String id = UUID.randomUUID().toString();
        client.setId(id);
        client.setActiveBookings(0);
        clients.put(id, client);
        bookings.put(id, ConcurrentHashMap.newKeySet()); 
        return client;
    }

    public Client getClient(String id) {
        Client client = clients.get(id);
        if (client == null) throw new NotFoundException("Клиент не найден");
        return client;
    }

    public List<Client> getClients(String type) {
        return clients.values().stream()
                .filter(c -> type == null || type.equals(c.getType().name()))
                .collect(Collectors.toList());
    }

    public synchronized Client updateClient(String id, Client updated) {
        validateClient(updated);
        Client existing = getClient(id);
        
        int newLimit = updated.getType().getLimit();
        if (existing.getActiveBookings() > newLimit) {
            throw new BusinessRuleException("Смена абонемента на " + updated.getType() + 
                " невозможна: у клиента уже " + existing.getActiveBookings() + " записей (лимит " + newLimit + ")");
        }
        
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        return existing;
    }

    public synchronized void deleteClient(String id) {
        if (clients.remove(id) == null) {
            throw new NotFoundException("Клиент не найден");
        }
        Set<String> clientBookings = bookings.remove(id);
        if (clientBookings != null) {
            for (String trainerId : clientBookings) {
                Trainer t = trainers.get(trainerId);
                if (t != null) t.setActiveBookings(t.getActiveBookings() - 1);
            }
        }
    }

    public synchronized void bookSession(String clientId, String trainerId) {
        Client client = getClient(clientId);
        Trainer trainer = getTrainer(trainerId);
        
        Set<String> clientBookings = bookings.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet());
        
        if (clientBookings.contains(trainerId)) {
            throw new BusinessRuleException("Клиент уже записан к этому тренеру");
        }

        if (trainer.getActiveBookings() >= trainer.getMaxCapacity()) {
            throw new BusinessRuleException("У тренера нет свободных мест");
        }

        int limit = client.getType().getLimit();
        if (client.getActiveBookings() >= limit) {
            throw new BusinessRuleException("Исчерпан лимит записей по абонементу " + client.getType());
        }

        clientBookings.add(trainerId);
        client.setActiveBookings(client.getActiveBookings() + 1);
        trainer.setActiveBookings(trainer.getActiveBookings() + 1);
    }

    private void validateClient(Client client) {
        if (client == null) throw new IllegalArgumentException("Тело запроса не может быть пустым");
        if (client.getName() == null || client.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя клиента не может быть пустым");
        }
        if (client.getType() == null) {
            throw new IllegalArgumentException("Тип абонемента должен быть BASIC или PREMIUM");
        }
    }

    private void validateTrainer(Trainer trainer) {
        if (trainer == null) throw new IllegalArgumentException("Тело запроса не может быть пустым");
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