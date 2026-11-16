package app.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.domain.Client;
import app.domain.ClientType;
import app.domain.Trainer;
import app.exception.BusinessRuleException;
import app.exception.NotFoundException;
import app.repository.InMemoryGymRepository;

class GymServiceTest {
    private GymService service;

    @BeforeEach
    void setUp() {
        service = new GymService(new InMemoryGymRepository());
    }

    @Test
    void createsReadsAndUpdatesTrainer() {
        Trainer created = service.createTrainer(
                new Trainer(null, "Анна", "Yoga", 3));

        Trainer updated = service.updateTrainer(created.getId(),
                new Trainer(null, "Анна Петрова", "Crossfit", 4));

        assertEquals(created.getId(), updated.getId());
        assertEquals("Анна Петрова", service.getTrainer(created.getId()).getName());
        assertEquals("Crossfit", service.getTrainer(created.getId()).getSpecialty());
        assertEquals(4, service.getTrainer(created.getId()).getMaxCapacity());
        assertEquals(0, service.getTrainer(created.getId()).getActiveBookings());
    }

    @Test
    void createsReadsAndUpdatesClient() {
        Client created = service.createClient(
                new Client(null, "Иван", ClientType.BASIC));

        Client updated = service.updateClient(created.getId(),
                new Client(null, "Иван Петров", ClientType.PREMIUM));

        assertEquals(created.getId(), updated.getId());
        assertEquals("Иван Петров", service.getClient(created.getId()).getName());
        assertEquals(ClientType.PREMIUM, service.getClient(created.getId()).getType());
        assertEquals(0, service.getClient(created.getId()).getActiveBookings());
    }

    @Test
    void bookingChangesClientAndTrainerState() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 1);
        Client client = createClient("Клиент", ClientType.BASIC);

        service.bookSession(client.getId(), trainer.getId());

        assertEquals(1, service.getClient(client.getId()).getActiveBookings());
        assertEquals(1, service.getTrainer(trainer.getId()).getActiveBookings());
    }

    @Test
    void rejectsDuplicateBooking() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 2);
        Client client = createClient("Клиент", ClientType.PREMIUM);
        service.bookSession(client.getId(), trainer.getId());

        assertThrows(BusinessRuleException.class,
                () -> service.bookSession(client.getId(), trainer.getId()));
        assertEquals(1, service.getClient(client.getId()).getActiveBookings());
        assertEquals(1, service.getTrainer(trainer.getId()).getActiveBookings());
    }

    @Test
    void rejectsBookingWhenTrainerCapacityReached() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 1);
        Client first = createClient("Первый", ClientType.BASIC);
        Client second = createClient("Второй", ClientType.BASIC);
        service.bookSession(first.getId(), trainer.getId());

        assertThrows(BusinessRuleException.class,
                () -> service.bookSession(second.getId(), trainer.getId()));
        assertEquals(0, service.getClient(second.getId()).getActiveBookings());
        assertEquals(1, service.getTrainer(trainer.getId()).getActiveBookings());
    }

    @Test
    void rejectsBookingWhenClientLimitReached() {
        Trainer first = createTrainer("Первый", "Yoga", 1);
        Trainer second = createTrainer("Второй", "Boxing", 1);
        Client client = createClient("Клиент", ClientType.BASIC);
        service.bookSession(client.getId(), first.getId());

        assertThrows(BusinessRuleException.class,
                () -> service.bookSession(client.getId(), second.getId()));
        assertEquals(1, service.getClient(client.getId()).getActiveBookings());
        assertEquals(0, service.getTrainer(second.getId()).getActiveBookings());
    }

    @Test
    void allowsPremiumClientUpToFiveBookingsAndRejectsSixth() {
        Client premium = createClient("Премиум", ClientType.PREMIUM);
        for (int i = 1; i <= 5; i++) {
            Trainer trainer = createTrainer("Тренер " + i, "Yoga", 1);
            service.bookSession(premium.getId(), trainer.getId());
        }
        assertEquals(5, service.getClient(premium.getId()).getActiveBookings());

        Trainer sixthTrainer = createTrainer("Тренер 6", "Boxing", 1);
        assertThrows(BusinessRuleException.class,
                () -> service.bookSession(premium.getId(), sixthTrainer.getId()));
        assertEquals(5, service.getClient(premium.getId()).getActiveBookings());
        assertEquals(0, service.getTrainer(sixthTrainer.getId()).getActiveBookings());
    }

    @Test
    void rejectsTrainerCapacityBelowExistingBookings() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 2);
        Client first = createClient("Первый", ClientType.BASIC);
        Client second = createClient("Второй", ClientType.BASIC);
        service.bookSession(first.getId(), trainer.getId());
        service.bookSession(second.getId(), trainer.getId());

        assertThrows(BusinessRuleException.class, () -> service.updateTrainer(
                trainer.getId(), new Trainer(null, "Тренер", "Yoga", 1)));
        assertEquals(2, service.getTrainer(trainer.getId()).getMaxCapacity());
        assertEquals(2, service.getTrainer(trainer.getId()).getActiveBookings());
    }

    @Test
    void rejectsClientDowngradeBelowExistingBookings() {
        Trainer first = createTrainer("Первый", "Yoga", 1);
        Trainer second = createTrainer("Второй", "Boxing", 1);
        Client client = createClient("Клиент", ClientType.PREMIUM);
        service.bookSession(client.getId(), first.getId());
        service.bookSession(client.getId(), second.getId());

        assertThrows(BusinessRuleException.class, () -> service.updateClient(
                client.getId(), new Client(null, "Клиент", ClientType.BASIC)));
        assertEquals(ClientType.PREMIUM, service.getClient(client.getId()).getType());
        assertEquals(2, service.getClient(client.getId()).getActiveBookings());
    }

    @Test
    void allowsUpdatingLimitsEqualToCurrentActiveBookings() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 3);
        Client client = createClient("Клиент", ClientType.PREMIUM);
        Client other = createClient("Второй", ClientType.BASIC);

        service.bookSession(client.getId(), trainer.getId());
        service.bookSession(other.getId(), trainer.getId());

        Trainer updatedTrainer = service.updateTrainer(
                trainer.getId(), new Trainer(null, "Тренер", "Yoga", 2));
        assertEquals(2, updatedTrainer.getMaxCapacity());
        assertEquals(2, service.getTrainer(trainer.getId()).getMaxCapacity());

        Client downgradedClient = service.updateClient(
                client.getId(), new Client(null, "Клиент", ClientType.BASIC));
        assertEquals(ClientType.BASIC, downgradedClient.getType());
        assertEquals(ClientType.BASIC, service.getClient(client.getId()).getType());

        Trainer anotherTrainer = createTrainer("Другой", "Boxing", 1);
        assertThrows(BusinessRuleException.class,
                () -> service.bookSession(client.getId(), anotherTrainer.getId()));
    }

    @Test
    void deletingTrainerRemovesBookingAndReleasesClientLimit() {
        Trainer first = createTrainer("Первый", "Crossfit", 1);
        Trainer second = createTrainer("Второй", "Yoga", 1);
        Client client = createClient("Клиент", ClientType.BASIC);
        service.bookSession(client.getId(), first.getId());

        service.deleteTrainer(first.getId());

        assertThrows(NotFoundException.class, () -> service.getTrainer(first.getId()));
        assertEquals(0, service.getClient(client.getId()).getActiveBookings());
        service.bookSession(client.getId(), second.getId());
        assertEquals(1, service.getClient(client.getId()).getActiveBookings());
        assertEquals(1, service.getTrainer(second.getId()).getActiveBookings());
    }

    @Test
    void deletingClientReleasesTrainerCapacity() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 1);
        Client client = createClient("Клиент", ClientType.BASIC);
        service.bookSession(client.getId(), trainer.getId());

        service.deleteClient(client.getId());

        assertEquals(0, service.getTrainer(trainer.getId()).getActiveBookings());
        assertThrows(NotFoundException.class, () -> service.getClient(client.getId()));

        Client replacement = createClient("Новый клиент", ClientType.BASIC);
        service.bookSession(replacement.getId(), trainer.getId());
        assertEquals(1, service.getTrainer(trainer.getId()).getActiveBookings());
    }

    @Test
    void reportsMissingResources() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 1);
        Client client = createClient("Клиент", ClientType.BASIC);

        assertThrows(NotFoundException.class, () -> service.getTrainer("missing"));
        assertThrows(NotFoundException.class, () -> service.getClient("missing"));
        assertThrows(NotFoundException.class, () -> service.updateTrainer(
                "missing", new Trainer(null, "Анна", "Yoga", 2)));
        assertThrows(NotFoundException.class, () -> service.deleteTrainer("missing"));
        assertThrows(NotFoundException.class, () -> service.updateClient(
                "missing", new Client(null, "Иван", ClientType.BASIC)));
        assertThrows(NotFoundException.class, () -> service.deleteClient("missing"));
        assertThrows(NotFoundException.class,
                () -> service.bookSession("missing", trainer.getId()));
        assertThrows(NotFoundException.class,
                () -> service.bookSession(client.getId(), "missing"));
    }

    @Test
    void validatesNullEmptyAndBoundaryInput() {
        assertThrows(IllegalArgumentException.class, () -> service.createTrainer(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createTrainer(new Trainer(null, " ", "Yoga", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createTrainer(new Trainer(null, "Тренер", " ", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createTrainer(new Trainer(null, "Тренер", "Yoga", 0)));
        assertThrows(IllegalArgumentException.class, () -> service.createClient(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new Client(null, " ", ClientType.BASIC)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new Client(null, "Клиент", null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.getClients("UNKNOWN"));
    }

    @Test
    void validatesInputOnUpdate() {
        Trainer trainer = createTrainer("Тренер", "Yoga", 2);
        Client client = createClient("Клиент", ClientType.BASIC);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateTrainer(trainer.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateTrainer(trainer.getId(), new Trainer(null, " ", "Yoga", 2)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateTrainer(trainer.getId(), new Trainer(null, "Тренер", " ", 2)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateTrainer(trainer.getId(), new Trainer(null, "Тренер", "Yoga", -1)));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateClient(client.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateClient(client.getId(), new Client(null, "", ClientType.BASIC)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateClient(client.getId(), new Client(null, "Клиент", null)));
    }

    @Test
    void filtersAreOptionalAndCaseInsensitive() {
        createTrainer("Первый", "Crossfit", 1);
        createTrainer("Второй", "Yoga", 1);
        createClient("Первый", ClientType.BASIC);
        createClient("Второй", ClientType.PREMIUM);

        assertEquals(2, service.getTrainers(null).size());
        assertEquals(1, service.getTrainers("crossfit").size());
        assertEquals(2, service.getClients(null).size());
        assertEquals(1, service.getClients("basic").size());
    }

    @Test
    void returnedObjectsCannotMutateStoredState() {
        Trainer created = createTrainer("Тренер", "Yoga", 1);
        Client client = createClient("Клиент", ClientType.BASIC);

        created.setName("Изменён снаружи");
        client.setName("Изменён снаружи");
        service.getTrainer(created.getId()).setSpecialty("Изменена снаружи");
        service.getClient(client.getId()).setType(ClientType.PREMIUM);

        assertEquals("Тренер", service.getTrainer(created.getId()).getName());
        assertEquals("Yoga", service.getTrainer(created.getId()).getSpecialty());
        assertEquals("Клиент", service.getClient(client.getId()).getName());
        assertEquals(ClientType.BASIC, service.getClient(client.getId()).getType());
    }

    @Test
    void concurrentReadsSeeCompleteTrainerSnapshots() throws Exception {
        Trainer created = createTrainer("A", "Yoga", 2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> writer = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 2_000; i++) {
                    Trainer state = (i & 1) == 0
                            ? new Trainer(null, "A", "Yoga", 2)
                            : new Trainer(null, "B", "Boxing", 3);
                    service.updateTrainer(created.getId(), state);
                }
            });
            Future<?> reader = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 2_000; i++) {
                    Trainer snapshot = service.getTrainer(created.getId());
                    boolean stateA = snapshot.getName().equals("A")
                            && snapshot.getSpecialty().equals("Yoga")
                            && snapshot.getMaxCapacity() == 2;
                    boolean stateB = snapshot.getName().equals("B")
                            && snapshot.getSpecialty().equals("Boxing")
                            && snapshot.getMaxCapacity() == 3;
                    assertTrue(stateA || stateB, "Получено частично обновлённое состояние");
                }
            });

            start.countDown();
            writer.get();
            reader.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private Trainer createTrainer(String name, String specialty, int maxCapacity) {
        return service.createTrainer(new Trainer(null, name, specialty, maxCapacity));
    }

    private Client createClient(String name, ClientType type) {
        return service.createClient(new Client(null, name, type));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }
}