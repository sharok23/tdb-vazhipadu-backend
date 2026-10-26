package com.tdb.vazhipadu.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.contract.request.OrderRequest;
import com.tdb.vazhipadu.contract.response.OrderResponse;
import com.tdb.vazhipadu.contract.response.OrderSummaryResponse;
import com.tdb.vazhipadu.contract.response.PageResponse;
import com.tdb.vazhipadu.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api")
@Tag(name = "Orders", description = "Place and view vazhipadu orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/temples/{templeId}/orders")
    @Operation(summary = "Place an order; the server calculates the total")
    @ApiResponse(responseCode = "201", description = "Order placed")
    @ApiResponse(responseCode = "400", description = "Invalid input or past booking date")
    @ApiResponse(responseCode = "404", description = "Temple or offering not found")
    @ApiResponse(responseCode = "409", description = "Daily limit for an offering would be exceeded")
    @ApiResponse(responseCode = "422", description = "Offering is not active")
    @ApiResponse(responseCode = "503", description = "Offering is busy with other orders; retry")
    public ResponseEntity<OrderResponse> place(@PathVariable Long templeId, @Valid @RequestBody OrderRequest request) {
        OrderResponse placed = orderService.place(templeId, request);
        return ResponseEntity.created(URI.create("/api/orders/" + placed.id())).body(placed);
    }

    @GetMapping("/temples/{templeId}/orders")
    @Operation(summary = "Search a temple's orders by date, status and phone, in pages ordered by id")
    @ApiResponse(responseCode = "200", description = "One page of matching orders")
    @ApiResponse(responseCode = "400", description = "Invalid filter or paging value")
    @ApiResponse(responseCode = "404", description = "Temple not found")
    public PageResponse<OrderSummaryResponse> search(
            @PathVariable Long templeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String phone,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.search(templeId, date, status, phone, page, size);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get an order by id")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public OrderResponse get(@PathVariable Long orderId) {
        return orderService.get(orderId);
    }
}
