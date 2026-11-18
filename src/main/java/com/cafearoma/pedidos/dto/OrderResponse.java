package com.cafearoma.pedidos.dto;

import com.cafearoma.pedidos.model.CustomerOrder;
import com.cafearoma.pedidos.model.OrderStatus;
import com.cafearoma.pedidos.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pedido tal como lo ven la API y la pantalla.
 * nextStatus y cancellable salen de OrderStatus, así la interfaz no repite las reglas.
 */
public final class OrderResponse {

    private final Long id;
    private final String orderNumber;
    private final String customerName;
    private final OrderStatus status;
    private final String statusLabel;
    private final PaymentStatus paymentStatus;
    private final String paymentStatusLabel;
    private final boolean paid;
    private final BigDecimal total;
    private final LocalDateTime createdAt;
    private final List<OrderItemResponse> items;
    private final OrderStatus nextStatus;
    private final String nextStatusLabel;
    private final boolean cancellable;

    public OrderResponse(Long id, String orderNumber, String customerName, OrderStatus status, String statusLabel,
                         PaymentStatus paymentStatus, String paymentStatusLabel, boolean paid, BigDecimal total,
                         LocalDateTime createdAt, List<OrderItemResponse> items, OrderStatus nextStatus,
                         String nextStatusLabel, boolean cancellable) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.status = status;
        this.statusLabel = statusLabel;
        this.paymentStatus = paymentStatus;
        this.paymentStatusLabel = paymentStatusLabel;
        this.paid = paid;
        this.total = total;
        this.createdAt = createdAt;
        this.items = items;
        this.nextStatus = nextStatus;
        this.nextStatusLabel = nextStatusLabel;
        this.cancellable = cancellable;
    }

    public static OrderResponse from(CustomerOrder order) {
        OrderStatus status = order.getStatus();
        OrderStatus next = status.nextStep();
        return new OrderResponse(
                order.getId(),
                "#" + String.format("%04d", order.getId()),
                order.getCustomerName(),
                status,
                status.getLabel(),
                order.getPaymentStatus(),
                order.getPaymentStatus().getLabel(),
                order.getPaymentStatus() == PaymentStatus.PAGADO,
                order.getTotal(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()),
                next,
                next == null ? null : next.getLabel(),
                status.isCancellable());
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentStatusLabel() {
        return paymentStatusLabel;
    }

    public boolean isPaid() {
        return paid;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public OrderStatus getNextStatus() {
        return nextStatus;
    }

    public String getNextStatusLabel() {
        return nextStatusLabel;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderResponse)) return false;
        OrderResponse that = (OrderResponse) o;
        return paid == that.paid && cancellable == that.cancellable && Objects.equals(id, that.id)
                && Objects.equals(orderNumber, that.orderNumber) && Objects.equals(customerName, that.customerName)
                && status == that.status && Objects.equals(statusLabel, that.statusLabel)
                && paymentStatus == that.paymentStatus && Objects.equals(paymentStatusLabel, that.paymentStatusLabel)
                && Objects.equals(total, that.total) && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(items, that.items) && nextStatus == that.nextStatus
                && Objects.equals(nextStatusLabel, that.nextStatusLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNumber, customerName, status, statusLabel, paymentStatus, paymentStatusLabel,
                paid, total, createdAt, items, nextStatus, nextStatusLabel, cancellable);
    }
}
