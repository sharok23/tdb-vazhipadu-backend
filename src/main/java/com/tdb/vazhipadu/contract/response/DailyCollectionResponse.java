package com.tdb.vazhipadu.contract.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyCollectionResponse(Long templeId, LocalDate date, BigDecimal totalAmount,
                                      List<OfferingCollectionResponse> offerings) {
}
