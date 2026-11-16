package app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import app.handler.ClientHandler;
import app.handler.TrainerHandler;
import app.service.GymService;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        GymService gymService = new GymService();
        server.createContext("/trainers", new TrainerHandler(gymService));
        server.createContext("/clients", new ClientHandler(gymService));

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()));
        server.setExecutor(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdown();
        }));

        server.start();
        System.out.println("Сервер запущен: http://localhost:" + PORT + "/ping");
    }
}
