package com.tdb.vazhipadu.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tdb.vazhipadu.constant.Nakshatra;
import com.tdb.vazhipadu.constant.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "temple_id", nullable = false)
    private Long templeId;

    @Column(name = "devotee_name", nullable = false, length = 100)
    private String devoteeName;

    @Column(nullable = false, length = 10)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Nakshatra nakshatra;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(Long templeId, String devoteeName, String phone, Nakshatra nakshatra, LocalDate bookingDate,
                 Instant createdAt) {
        this.templeId = templeId;
        this.devoteeName = devoteeName;
        this.phone = phone;
        this.nakshatra = nakshatra;
        this.bookingDate = bookingDate;
        this.createdAt = createdAt;
        this.status = OrderStatus.BOOKED;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addLine(Offering offering, int quantity) {
        OrderLine line = new OrderLine(this, offering, quantity);
        lines.add(line);
        totalAmount = totalAmount.add(line.getLineTotal());
    }

    public void cancel() {
        status = OrderStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getTempleId() {
        return templeId;
    }

    public String getDevoteeName() {
        return devoteeName;
    }

    public String getPhone() {
        return phone;
    }

    public Nakshatra getNakshatra() {
        return nakshatra;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }
}
