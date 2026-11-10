package app.handler;

import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import app.domain.Trainer;
import app.service.GymService;

public class TrainerHandler extends BaseHandler {
    private final GymService gymService;

    public TrainerHandler(GymService gymService) {
        this.gymService = gymService;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (method.equals("POST") && parts.length == 2) {
            Trainer trainer = parseBody(exchange, Trainer.class);
            Trainer created = gymService.createTrainer(trainer);
            sendJson(exchange, 201, created);
            
        } else if (method.equals("GET") && parts.length == 2) {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            sendJson(exchange, 200, gymService.getTrainers(params.get("specialty")));
            
        } else if (method.equals("GET") && parts.length == 3) {
            sendJson(exchange, 200, gymService.getTrainer(parts[2]));
            
        } else if (method.equals("PUT") && parts.length == 3) {
            Trainer trainer = parseBody(exchange, Trainer.class);
            Trainer updated = gymService.updateTrainer(parts[2], trainer);
            sendJson(exchange, 200, updated);
            
        } else if (method.equals("DELETE") && parts.length == 3) {
            gymService.deleteTrainer(parts[2]);
            exchange.sendResponseHeaders(204, -1);
            
        } else {
            exchange.getResponseHeaders().add("Allow", "GET, POST, PUT, DELETE");
            sendError(exchange, 405, "Метод не поддерживается");
        }
    }
}