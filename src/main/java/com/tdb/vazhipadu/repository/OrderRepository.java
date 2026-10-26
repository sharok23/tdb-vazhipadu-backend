package com.tdb.vazhipadu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tdb.vazhipadu.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
