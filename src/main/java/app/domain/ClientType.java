package app.domain;

public enum ClientType {
    BASIC(1),
    PREMIUM(5);

    private final int limit;

    ClientType(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }
}