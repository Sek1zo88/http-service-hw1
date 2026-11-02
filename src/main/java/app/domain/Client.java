package app.domain;

public class Client {
    private String id;
    private String name;
    private ClientType type;
    private int activeBookings;

    public Client() {}

    public Client(String id, String name, ClientType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.activeBookings = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ClientType getType() { return type; }
    public void setType(ClientType type) { this.type = type; }
    public int getActiveBookings() { return activeBookings; }
    public void setActiveBookings(int activeBookings) { this.activeBookings = activeBookings; }
}