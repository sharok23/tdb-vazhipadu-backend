package com.tdb.vazhipadu.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "offering")
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "temple_id", nullable = false)
    private Long templeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    protected Offering() {
    }

    public Offering(Long templeId, String name, BigDecimal price, boolean active, Integer dailyLimit) {
        this.templeId = templeId;
        update(name, price, active, dailyLimit);
    }

    public final void update(String name, BigDecimal price, boolean active, Integer dailyLimit) {
        this.name = name;
        this.price = price;
        this.active = active;
        this.dailyLimit = dailyLimit;
    }

    public Long getId() {
        return id;
    }

    public Long getTempleId() {
        return templeId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }
}
