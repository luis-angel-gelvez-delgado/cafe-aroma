package com.cafearoma.pedidos.dto;

import com.cafearoma.pedidos.model.Modifier;
import com.cafearoma.pedidos.model.Size;

import java.util.List;
import java.util.Objects;

/** Un ítem del pedido: producto, tamaño (null en panadería) y 0..n modificadores. */
public class OrderItemRequest {

    private Long productId;
    private Size size;
    private List<Modifier> modifiers;

    /** Constructor vacío: Jackson lo usa para leer el JSON. */
    public OrderItemRequest() {
    }

    public OrderItemRequest(Long productId, Size size, List<Modifier> modifiers) {
        this.productId = productId;
        this.size = size;
        this.modifiers = modifiers;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemRequest)) return false;
        OrderItemRequest that = (OrderItemRequest) o;
        return Objects.equals(productId, that.productId) && size == that.size
                && Objects.equals(modifiers, that.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, size, modifiers);
    }
}
