package com.planner.app.controller;

import com.planner.app.dto.VoyageDTO;
import com.planner.app.entity.Voyage;
import com.planner.app.service.VoyageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voyages")
@RequiredArgsConstructor
public class VoyageController {
    private final VoyageService voyageService;

    @GetMapping("/{id}")
    public ResponseEntity<Voyage> getVoyage(@PathVariable Integer id) {
        try {
            Voyage voyage = voyageService.getVoyageById(id);
            return ResponseEntity.ok(voyage);
        } catch ( RuntimeException e ) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<Voyage> createVoyage(@RequestBody VoyageDTO voyageDTO) {
        try {
            Voyage newvoyage = new Voyage();
            newvoyage.setObjectType(voyageDTO.getObjectType());
            newvoyage.setObjectId(voyageDTO.getObjectId());
            newvoyage.setDestination(voyageDTO.getDestination());
            newvoyage.setBudgetTotal(voyageDTO.getBudgetTotal());
            newvoyage.setDurationDays(voyageDTO.getDurationDays());
            newvoyage.setStartDate(voyageDTO.getStartDate());
            newvoyage.setCoverImage(voyageDTO.getCoverImage());

            Voyage voyage = voyageService.createVoyage(newvoyage);
            return ResponseEntity.status(HttpStatus.CREATED).body(voyage);
        } catch ( RuntimeException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
