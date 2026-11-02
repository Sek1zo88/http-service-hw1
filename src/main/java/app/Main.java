package app;

import app.handler.ClientHandler;
import app.handler.TrainerHandler;
import app.service.GymService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        GymService gymService = new GymService();
        server.createContext("/trainers", new TrainerHandler(gymService));
        server.createContext("/clients", new ClientHandler(gymService));

        server.start();
        System.out.println("Сервер запущен: http://localhost:" + PORT + "/ping");
    }
}