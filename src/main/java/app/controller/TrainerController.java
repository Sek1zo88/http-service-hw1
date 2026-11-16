package app.controller;

import app.api.TrainerRequest;
import app.api.TrainerResponse;
import app.service.GymService;
import jakarta.validation.Valid;

import java.util.List;

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
@RequestMapping("/trainers")
public class TrainerController {
    private final GymService gymService;

    public TrainerController(GymService gymService) {
        this.gymService = gymService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerResponse create(@Valid @RequestBody TrainerRequest request) {
        return TrainerResponse.from(gymService.createTrainer(request.toDomain()));
    }

    @GetMapping
    public List<TrainerResponse> getAll(
            @RequestParam(required = false) String specialty) {
        return gymService.getTrainers(specialty).stream()
                .map(TrainerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TrainerResponse getById(@PathVariable String id) {
        return TrainerResponse.from(gymService.getTrainer(id));
    }

    @PutMapping("/{id}")
    public TrainerResponse update(
            @PathVariable String id,
            @Valid @RequestBody TrainerRequest request) {
        return TrainerResponse.from(gymService.updateTrainer(id, request.toDomain()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        gymService.deleteTrainer(id);
    }
}
