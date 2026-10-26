package com.tdb.vazhipadu.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdb.vazhipadu.contract.request.OfferingRequest;
import com.tdb.vazhipadu.contract.response.OfferingResponse;
import com.tdb.vazhipadu.service.OfferingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/temples/{templeId}/offerings")
@Tag(name = "Offerings", description = "Manage a temple's offering catalogue")
public class OfferingController {

    private final OfferingService offeringService;

    public OfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @PostMapping
    @Operation(summary = "Add an offering to a temple")
    @ApiResponse(responseCode = "201", description = "Offering created")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Temple not found")
    @ApiResponse(responseCode = "409", description = "Offering name already exists for this temple")
    public ResponseEntity<OfferingResponse> create(@PathVariable Long templeId,
                                                   @Valid @RequestBody OfferingRequest request) {
        OfferingResponse created = offeringService.create(templeId, request);
        URI location = URI.create("/api/temples/%d/offerings/%d".formatted(templeId, created.id()));
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List a temple's offerings, including inactive ones")
    @ApiResponse(responseCode = "200", description = "Offerings ordered by id")
    @ApiResponse(responseCode = "404", description = "Temple not found")
    public List<OfferingResponse> list(@PathVariable Long templeId) {
        return offeringService.list(templeId);
    }

    @PutMapping("/{offeringId}")
    @Operation(summary = "Replace an offering's name, price, active flag and daily limit")
    @ApiResponse(responseCode = "200", description = "Offering updated")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Offering not found for this temple")
    @ApiResponse(responseCode = "409", description = "Offering name already exists for this temple")
    public OfferingResponse update(@PathVariable Long templeId, @PathVariable Long offeringId,
                                   @Valid @RequestBody OfferingRequest request) {
        return offeringService.update(templeId, offeringId, request);
    }
}
