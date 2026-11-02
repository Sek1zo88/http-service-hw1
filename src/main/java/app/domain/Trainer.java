package app.domain;

public class Trainer {
    private String id;
    private String name;
    private String specialty;
    private int activeBookings;
    private int maxCapacity;

    public Trainer() {}

    public Trainer(String id, String name, String specialty, int maxCapacity) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.maxCapacity = maxCapacity;
        this.activeBookings = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public int getActiveBookings() { return activeBookings; }
    public void setActiveBookings(int activeBookings) { this.activeBookings = activeBookings; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
}