package com.tdb.vazhipadu.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.model.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("""
            select coalesce(sum(l.quantity), 0) from OrderLine l
            where l.offeringId = :offeringId and l.order.bookingDate = :date and l.order.status = :status
            """)
    long sumQuantity(@Param("offeringId") Long offeringId, @Param("date") LocalDate date,
                     @Param("status") OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
