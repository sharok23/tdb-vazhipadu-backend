package com.tdb.vazhipadu.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdb.vazhipadu.contract.request.OrderRequest;
import com.tdb.vazhipadu.contract.response.OrderResponse;
import com.tdb.vazhipadu.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
    @ApiResponse(responseCode = "422", description = "Offering is not active")
    public ResponseEntity<OrderResponse> place(@PathVariable Long templeId, @Valid @RequestBody OrderRequest request) {
        OrderResponse placed = orderService.place(templeId, request);
        return ResponseEntity.created(URI.create("/api/orders/" + placed.id())).body(placed);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get an order by id")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public OrderResponse get(@PathVariable Long orderId) {
        return orderService.get(orderId);
    }
}
