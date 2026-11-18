package com.cafearoma.pedidos.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pedido de mostrador. Se llama "CustomerOrder" porque "Order" es palabra reservada en SQL y JPQL.
 * Las reglas de transición viven en OrderStatus y se aplican en OrderService.
 */
@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 60)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatus paymentStatus;

    /** Suma de los precios de los ítems; se calcula en el backend. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
        // requerido por JPA
    }

    public CustomerOrder(String customerName) {
        this.customerName = customerName;
        this.status = OrderStatus.RECIBIDO;
        this.paymentStatus = PaymentStatus.PENDIENTE;
        this.total = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    /** Agrega un ítem y actualiza el total: total = suma de los ítems (regla 2 del reto). */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
        total = total.add(item.getPrice());
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    /** No valida: usar siempre los métodos de OrderService (take, markReady, deliver, cancel), que verifican el paso. */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAGADO;
    }

    // Reglas del flujo caja/cocina. OrderService las aplica y OrderResponse las expone
    // para que la pantalla solo muestre los botones permitidos.

    /** Caja cobra: solo si está pendiente y el pedido no terminó. Un pago nunca vuelve a Pendiente. */
    public boolean canBePaid() {
        return !isPaid() && status != OrderStatus.CANCELADO && status != OrderStatus.ENTREGADO;
    }

    /** Caja cancela: solo en Recibido y sin pagar. */
    public boolean canBeCancelled() {
        return status.isCancellable() && !isPaid();
    }

    /** Cocina toma el pedido: Recibido → En preparación, solo si ya está pagado. */
    public boolean canBeTaken() {
        return status.canTransitionTo(OrderStatus.EN_PREPARACION) && isPaid();
    }

    /** Cocina lo marca listo: En preparación → Listo para recoger. */
    public boolean canBeMarkedReady() {
        return status.canTransitionTo(OrderStatus.LISTO_PARA_RECOGER);
    }

    /** Caja entrega: Listo para recoger → Entregado, siempre pagado. */
    public boolean canBeDelivered() {
        return status.canTransitionTo(OrderStatus.ENTREGADO) && isPaid();
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
