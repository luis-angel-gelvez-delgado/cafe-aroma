package com.cafearoma.pedidos.dto;

import com.cafearoma.pedidos.model.OrderStatus;

public class UpdateStatusRequest {

    private OrderStatus status;

    /** Constructor vacío: Jackson lo usa para leer el JSON. */
    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateStatusRequest)) return false;
        return status == ((UpdateStatusRequest) o).status;
    }

    @Override
    public int hashCode() {
        return status == null ? 0 : status.hashCode();
    }
}
