package com.tdb.vazhipadu.contract.request;

import java.time.LocalDate;
import java.util.List;

import com.tdb.vazhipadu.constant.Nakshatra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderRequest(
        @NotBlank @Size(max = 100) String devoteeName,
        @NotNull @Pattern(regexp = "[6-9]\\d{9}", message = "must be a 10-digit Indian mobile number") String phone,
        @NotNull Nakshatra nakshatra,
        @NotNull LocalDate bookingDate,
        @NotEmpty @Size(max = 20) List<@NotNull @Valid OrderItemRequest> items) {
}
