package com.cafearoma.pedidos.dto;

import com.cafearoma.pedidos.model.PaymentStatus;

public class UpdatePaymentRequest {

    private PaymentStatus paymentStatus;

    /** Constructor vacío: Jackson lo usa para leer el JSON. */
    public UpdatePaymentRequest() {
    }

    public UpdatePaymentRequest(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdatePaymentRequest)) return false;
        return paymentStatus == ((UpdatePaymentRequest) o).paymentStatus;
    }

    @Override
    public int hashCode() {
        return paymentStatus == null ? 0 : paymentStatus.hashCode();
    }
}
