package com.tdb.vazhipadu.contract.response;

import java.math.BigDecimal;

import com.tdb.vazhipadu.model.Offering;

public record OfferingResponse(Long id, Long templeId, String name, BigDecimal price, boolean active,
                               Integer dailyLimit) {

    public static OfferingResponse from(Offering offering) {
        return new OfferingResponse(offering.getId(), offering.getTempleId(), offering.getName(),
                offering.getPrice(), offering.isActive(), offering.getDailyLimit());
    }
}
