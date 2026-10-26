package com.tdb.vazhipadu.contract.response;

import java.math.BigDecimal;

import com.tdb.vazhipadu.model.OrderLine;

public record OrderLineResponse(Long offeringId, String offeringName, BigDecimal unitPrice, int quantity,
                                BigDecimal lineTotal) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getOfferingId(), line.getOfferingName(), line.getUnitPrice(),
                line.getQuantity(), line.getLineTotal());
    }
}
