package com.fashion.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    private String orderNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "ship_full_name", nullable = false, length = 120)
    private String shipFullName;

    @Column(name = "ship_phone", nullable = false, length = 20)
    private String shipPhone;

    @Column(name = "ship_line1", nullable = false, length = 200)
    private String shipLine1;

    @Column(name = "ship_line2", length = 200)
    private String shipLine2;

    @Column(name = "ship_city", nullable = false, length = 80)
    private String shipCity;

    @Column(name = "ship_state", nullable = false, length = 80)
    private String shipState;

    @Column(name = "ship_postal_code", nullable = false, length = 20)
    private String shipPostalCode;

    @Column(name = "ship_country", nullable = false, length = 80)
    private String shipCountry;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCharge;

    @Column(name = "final_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalTotal;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getShipFullName() {
        return shipFullName;
    }

    public void setShipFullName(String shipFullName) {
        this.shipFullName = shipFullName;
    }

    public String getShipPhone() {
        return shipPhone;
    }

    public void setShipPhone(String shipPhone) {
        this.shipPhone = shipPhone;
    }

    public String getShipLine1() {
        return shipLine1;
    }

    public void setShipLine1(String shipLine1) {
        this.shipLine1 = shipLine1;
    }

    public String getShipLine2() {
        return shipLine2;
    }

    public void setShipLine2(String shipLine2) {
        this.shipLine2 = shipLine2;
    }

    public String getShipCity() {
        return shipCity;
    }

    public void setShipCity(String shipCity) {
        this.shipCity = shipCity;
    }

    public String getShipState() {
        return shipState;
    }

    public void setShipState(String shipState) {
        this.shipState = shipState;
    }

    public String getShipPostalCode() {
        return shipPostalCode;
    }

    public void setShipPostalCode(String shipPostalCode) {
        this.shipPostalCode = shipPostalCode;
    }

    public String getShipCountry() {
        return shipCountry;
    }

    public void setShipCountry(String shipCountry) {
        this.shipCountry = shipCountry;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getShippingCharge() {
        return shippingCharge;
    }

    public void setShippingCharge(BigDecimal shippingCharge) {
        this.shippingCharge = shippingCharge;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}
