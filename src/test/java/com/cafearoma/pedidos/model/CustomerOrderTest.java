package com.cafearoma.pedidos.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba unitaria de las reglas caja/cocina que dependen del pago. Sin Spring ni base de datos. */
class CustomerOrderTest {

    @Test
    void pedidoNuevoSePuedeCobrarYCancelarPeroCocinaNoLoToma() {
        CustomerOrder order = new CustomerOrder("Ana");
        assertTrue(order.canBePaid());
        assertTrue(order.canBeCancelled());
        assertFalse(order.canBeTaken());
    }

    @Test
    void pedidoPagadoNoSeCancelaYCocinaLoPuedeTomar() {
        CustomerOrder order = new CustomerOrder("Ana");
        order.setPaymentStatus(PaymentStatus.PAGADO);
        assertFalse(order.canBePaid());
        assertFalse(order.canBeCancelled());
        assertTrue(order.canBeTaken());
    }

    @Test
    void soloSeEntregaListoYPagado() {
        CustomerOrder order = new CustomerOrder("Ana");
        order.setPaymentStatus(PaymentStatus.PAGADO);
        order.setStatus(OrderStatus.LISTO_PARA_RECOGER);
        assertTrue(order.canBeDelivered());
        assertFalse(order.canBeMarkedReady());
    }

    @Test
    void pedidoCanceladoNoSeCobra() {
        CustomerOrder order = new CustomerOrder("Ana");
        order.setStatus(OrderStatus.CANCELADO);
        assertFalse(order.canBePaid());
    }
}
