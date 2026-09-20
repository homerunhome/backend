package org.zerozero.homerunhome.itinerary.controller;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.homerunhome.itinerary.dto.ItineraryRequest;
import org.zerozero.homerunhome.itinerary.dto.ItineraryResponse;
import org.zerozero.homerunhome.itinerary.service.ItineraryService;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping
    public ResponseEntity<ItineraryResponse> create(@Valid @RequestBody ItineraryRequest request) {
        ItineraryResponse response = itineraryService.create(request);
        return ResponseEntity.created(URI.create("/api/itineraries/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ItineraryResponse get(@PathVariable Long id) {
        return itineraryService.get(id);
    }
}
