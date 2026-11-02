package app.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import app.exception.BusinessRuleException;
import app.exception.NotFoundException;
import tools.jackson.databind.json.JsonMapper;

public abstract class BaseHandler implements HttpHandler {
    protected final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                handleRequest(exchange);
            } catch (NotFoundException e) {
                sendError(exchange, 404, e.getMessage());
            } catch (BusinessRuleException e) {
                sendError(exchange, 409, e.getMessage());
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, "Некорректный запрос: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Ошибка сервера: " + e.getMessage());
                sendError(exchange, 500, "Внутренняя ошибка сервера");
            }
        }
    }

    protected abstract void handleRequest(HttpExchange exchange) throws Exception;

    protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendJson(exchange, statusCode, Map.of("error", message));
    }

    protected Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) params.put(pair[0], pair[1]);
        }
        return params;
    }
}