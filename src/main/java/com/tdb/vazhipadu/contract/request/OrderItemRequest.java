package com.tdb.vazhipadu.contract.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull @Positive Long offeringId,
        @NotNull @Min(1) @Max(100) Integer quantity) {
}
