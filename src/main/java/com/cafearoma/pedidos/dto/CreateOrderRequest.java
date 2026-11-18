package com.cafearoma.pedidos.dto;

import java.util.List;
import java.util.Objects;

/** Datos para crear un pedido. El precio NO se recibe: lo calcula el backend. */
public class CreateOrderRequest {

    private String customerName;
    private List<OrderItemRequest> items;

    /** Constructor vacío: Jackson lo usa para leer el JSON. */
    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerName, List<OrderItemRequest> items) {
        this.customerName = customerName;
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateOrderRequest)) return false;
        CreateOrderRequest that = (CreateOrderRequest) o;
        return Objects.equals(customerName, that.customerName) && Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerName, items);
    }
}
