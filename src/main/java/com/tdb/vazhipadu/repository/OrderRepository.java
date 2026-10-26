package com.tdb.vazhipadu.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.contract.response.OfferingCollectionResponse;
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

    @Query("""
            select new com.tdb.vazhipadu.contract.response.OfferingCollectionResponse(
                o.id, o.name, sum(l.quantity), count(l), sum(l.lineTotal))
            from OrderLine l join l.order ord join Offering o on o.id = l.offeringId
            where ord.templeId = :templeId and ord.status = :status
              and ord.createdAt >= :from and ord.createdAt < :to
            group by o.id, o.name
            order by o.id
            """)
    List<OfferingCollectionResponse> collectionByOffering(@Param("templeId") Long templeId,
                                                          @Param("status") OrderStatus status,
                                                          @Param("from") Instant from, @Param("to") Instant to);
}
