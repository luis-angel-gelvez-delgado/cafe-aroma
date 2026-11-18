package com.cafearoma.pedidos.dto;

import com.cafearoma.pedidos.model.Modifier;
import com.cafearoma.pedidos.model.OrderItem;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Ítem de un pedido listo para mostrar (tamaño y modificadores como texto). */
public final class OrderItemResponse {

    private final String productName;
    private final String size;
    private final List<String> modifiers;
    private final BigDecimal price;

    public OrderItemResponse(String productName, String size, List<String> modifiers, BigDecimal price) {
        this.productName = productName;
        this.size = size;
        this.modifiers = modifiers;
        this.price = price;
    }

    public static OrderItemResponse from(OrderItem item) {
        List<String> modifiers = item.getModifiers().stream()
                .sorted(Comparator.comparingInt(Modifier::ordinal))
                .map(Modifier::getLabel)
                .collect(Collectors.toList());
        String size = item.getSize() == null ? null : item.getSize().getLabel();
        return new OrderItemResponse(item.getProduct().getName(), size, modifiers, item.getPrice());
    }

    public String getProductName() {
        return productName;
    }

    public String getSize() {
        return size;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemResponse)) return false;
        OrderItemResponse that = (OrderItemResponse) o;
        return Objects.equals(productName, that.productName) && Objects.equals(size, that.size)
                && Objects.equals(modifiers, that.modifiers) && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName, size, modifiers, price);
    }
}
