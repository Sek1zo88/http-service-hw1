package app.controller;

import app.api.ClientRequest;
import app.api.ClientResponse;
import app.service.GymService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private final GymService gymService;

    public ClientController(GymService gymService) {
        this.gymService = gymService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest request) {
        return ClientResponse.from(gymService.createClient(request.toDomain()));
    }

    @GetMapping
    public List<ClientResponse> getAll(@RequestParam(required = false) String type) {
        return gymService.getClients(type).stream()
                .map(ClientResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClientResponse getById(@PathVariable String id) {
        return ClientResponse.from(gymService.getClient(id));
    }

    @PutMapping("/{id}")
    public ClientResponse update(
            @PathVariable String id,
            @Valid @RequestBody ClientRequest request) {
        return ClientResponse.from(gymService.updateClient(id, request.toDomain()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        gymService.deleteClient(id);
    }

    @PostMapping("/{id}/book")
    public Map<String, String> book(
            @PathVariable String id,
            @RequestParam String trainerId) {
        if (trainerId.isBlank()) {
            throw new IllegalArgumentException("Параметр trainerId обязателен");
        }
        gymService.bookSession(id, trainerId);
        return Map.of("message", "Успешно записан на тренировку");
    }
}
