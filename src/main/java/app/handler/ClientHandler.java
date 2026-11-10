package app.handler;

import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import app.domain.Client;
import app.service.GymService;

public class ClientHandler extends BaseHandler {
    private final GymService gymService;

    public ClientHandler(GymService gymService) {
        this.gymService = gymService;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (method.equals("POST") && parts.length == 2) {
            Client client = parseBody(exchange, Client.class);
            Client created = gymService.createClient(client);
            sendJson(exchange, 201, created);
            
        } else if (method.equals("GET") && parts.length == 2) {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            sendJson(exchange, 200, gymService.getClients(params.get("type")));
            
        } else if (method.equals("GET") && parts.length == 3) {
            sendJson(exchange, 200, gymService.getClient(parts[2]));
            
        } else if (method.equals("PUT") && parts.length == 3) {
            Client client = parseBody(exchange, Client.class);
            Client updated = gymService.updateClient(parts[2], client);
            sendJson(exchange, 200, updated);
            
        } else if (method.equals("DELETE") && parts.length == 3) {
            gymService.deleteClient(parts[2]);
            exchange.sendResponseHeaders(204, -1);
            
        } else if (method.equals("POST") && parts.length == 4 && parts[3].equals("book")) {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String trainerId = params.get("trainerId");
            if (trainerId == null || trainerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Параметр trainerId обязателен");
            }
            gymService.bookSession(parts[2], trainerId);
            sendJson(exchange, 200, Map.of("message", "Успешно записан на тренировку"));
            
        } else {
            exchange.getResponseHeaders().add("Allow", "GET, POST, PUT, DELETE");
            sendError(exchange, 405, "Метод не поддерживается");
        }
    }
}