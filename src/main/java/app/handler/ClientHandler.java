package app.handler;

import app.domain.Client;
import app.domain.ClientType;
import app.service.GymService;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public class ClientHandler extends BaseHandler {
    private final GymService service;

    public ClientHandler(GymService service) { this.service = service; }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (method.equals("POST") && parts.length == 2) {
            Client client = mapper.readValue(exchange.getRequestBody(), Client.class);
            sendJson(exchange, 201, service.createClient(client));
        } else if (method.equals("GET") && parts.length == 2) {
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getQuery());
            ClientType type = queryParams.containsKey("type") ? ClientType.valueOf(queryParams.get("type").toUpperCase()) : null;
            sendJson(exchange, 200, service.getClients(type));
        } else if (method.equals("GET") && parts.length == 3) {
            sendJson(exchange, 200, service.getClient(parts[2]));
        } else if (method.equals("PUT") && parts.length == 3) {
            Client client = mapper.readValue(exchange.getRequestBody(), Client.class);
            sendJson(exchange, 200, service.updateClient(parts[2], client));
        } else if (method.equals("DELETE") && parts.length == 3) {
            service.deleteClient(parts[2]);
            exchange.sendResponseHeaders(204, -1);
        } else if (method.equals("POST") && parts.length == 4 && parts[3].equals("book")) {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String trainerId = params.get("trainerId");
            if (trainerId == null) throw new IllegalArgumentException("Не указан trainerId");
            
            service.bookSession(parts[2], trainerId);
            sendJson(exchange, 200, Map.of("message", "Успешно записан на тренировку"));
        } else {
            sendError(exchange, 405, "Метод не поддерживается");
        }
    }
}