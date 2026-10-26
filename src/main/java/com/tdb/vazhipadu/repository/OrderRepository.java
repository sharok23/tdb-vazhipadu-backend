package com.tdb.vazhipadu.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("""
            select coalesce(sum(l.quantity), 0) from OrderLine l
            where l.offeringId = :offeringId and l.order.bookingDate = :date and l.order.status = :status
            """)
    long sumQuantity(@Param("offeringId") Long offeringId, @Param("date") LocalDate date,
                     @Param("status") OrderStatus status);
}
