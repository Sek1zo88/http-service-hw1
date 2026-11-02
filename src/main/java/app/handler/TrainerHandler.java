package app.handler;

import app.domain.Trainer;
import app.service.GymService;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

public class TrainerHandler extends BaseHandler {
    private final GymService service;

    public TrainerHandler(GymService service) { this.service = service; }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (method.equals("POST") && parts.length == 2) {
            Trainer trainer = mapper.readValue(exchange.getRequestBody(), Trainer.class);
            sendJson(exchange, 201, service.createTrainer(trainer));
        } else if (method.equals("GET") && parts.length == 2) {
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getQuery());
            sendJson(exchange, 200, service.getTrainers(queryParams.get("specialty")));
        } else if (method.equals("GET") && parts.length == 3) {
            sendJson(exchange, 200, service.getTrainer(parts[2]));
        } else if (method.equals("PUT") && parts.length == 3) {
            Trainer trainer = mapper.readValue(exchange.getRequestBody(), Trainer.class);
            sendJson(exchange, 200, service.updateTrainer(parts[2], trainer));
        } else if (method.equals("DELETE") && parts.length == 3) {
            service.deleteTrainer(parts[2]);
            exchange.sendResponseHeaders(204, -1);
        } else {
            sendError(exchange, 405, "Метод не поддерживается");
        }
    }
}