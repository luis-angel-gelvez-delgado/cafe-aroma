package com.cafearoma.pedidos.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Estados del pedido y transiciones permitidas.
 * Recibido → En preparación → Listo para recoger → Entregado.
 * Cancelado solo desde Recibido (antes de pasar a preparación).
 */
public enum OrderStatus {
    RECIBIDO("Recibido"),
    EN_PREPARACION("En preparación"),
    LISTO_PARA_RECOGER("Listo para recoger"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canTransitionTo(OrderStatus next) {
        switch (this) {
            case RECIBIDO:
                return next == EN_PREPARACION || next == CANCELADO;
            case EN_PREPARACION:
                return next == LISTO_PARA_RECOGER;
            case LISTO_PARA_RECOGER:
                return next == ENTREGADO;
            default: // ENTREGADO y CANCELADO son estados finales
                return false;
        }
    }

    /** Siguiente paso del flujo normal, o null si el pedido ya terminó. */
    public OrderStatus nextStep() {
        switch (this) {
            case RECIBIDO:
                return EN_PREPARACION;
            case EN_PREPARACION:
                return LISTO_PARA_RECOGER;
            case LISTO_PARA_RECOGER:
                return ENTREGADO;
            default: // ENTREGADO y CANCELADO no tienen siguiente paso
                return null;
        }
    }

    public boolean isCancellable() {
        return canTransitionTo(CANCELADO);
    }

    private static final List<OrderStatus> ACTIVE =
            Collections.unmodifiableList(Arrays.asList(RECIBIDO, EN_PREPARACION, LISTO_PARA_RECOGER));
    private static final List<OrderStatus> CLOSED =
            Collections.unmodifiableList(Arrays.asList(ENTREGADO, CANCELADO));

    public static List<OrderStatus> activeStatuses() {
        return ACTIVE;
    }

    public static List<OrderStatus> closedStatuses() {
        return CLOSED;
    }
}
