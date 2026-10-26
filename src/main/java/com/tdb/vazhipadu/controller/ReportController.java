package com.tdb.vazhipadu.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdb.vazhipadu.contract.response.DailyCollectionResponse;
import com.tdb.vazhipadu.service.DailyCollectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/temples/{templeId}/reports")
@Tag(name = "Reports", description = "Collection reports for finance")
public class ReportController {

    private final DailyCollectionService dailyCollectionService;

    public ReportController(DailyCollectionService dailyCollectionService) {
        this.dailyCollectionService = dailyCollectionService;
    }

    @GetMapping("/daily-collection")
    @Operation(summary = "Amount and count per offering for orders placed on a date (IST), excluding cancelled")
    @ApiResponse(responseCode = "200", description = "Daily collection report")
    @ApiResponse(responseCode = "400", description = "Missing or invalid date")
    @ApiResponse(responseCode = "404", description = "Temple not found")
    public DailyCollectionResponse dailyCollection(
            @PathVariable Long templeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyCollectionService.report(templeId, date);
    }
}
