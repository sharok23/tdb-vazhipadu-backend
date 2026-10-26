package com.tdb.vazhipadu.contract.response;

import java.math.BigDecimal;

public record OfferingCollectionResponse(Long offeringId, String offeringName, Long quantity, Long orderCount,
                                         BigDecimal totalAmount) {
}
