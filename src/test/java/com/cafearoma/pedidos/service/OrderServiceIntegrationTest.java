package com.cafearoma.pedidos.service;

import com.cafearoma.pedidos.dto.CreateOrderRequest;
import com.cafearoma.pedidos.dto.OrderItemRequest;
import com.cafearoma.pedidos.dto.OrderResponse;
import com.cafearoma.pedidos.model.Modifier;
import com.cafearoma.pedidos.model.OrderStatus;
import com.cafearoma.pedidos.model.PaymentStatus;
import com.cafearoma.pedidos.model.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración: levanta Spring con una base H2 en memoria (perfil "test"),
 * ejecuta schema.sql y data.sql, y prueba el servicio con persistencia real.
 * @Transactional deshace los cambios al final de cada prueba.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    // ids de data.sql
    private static final long LATTE = 4L;
    private static final long FRAPPE = 6L;
    private static final long CROISSANT = 8L;

    @Autowired
    private OrderService orderService;

    @Test
    void creaPedidoValidoYCalculaElTotal() {
        OrderResponse created = orderService.createOrder(new CreateOrderRequest("Ana", Arrays.asList(
                new OrderItemRequest(LATTE, Size.GRANDE, Arrays.asList(Modifier.SHOT_EXTRA, Modifier.LECHE_DESLACTOSADA)),
                new OrderItemRequest(CROISSANT, null, Arrays.asList(Modifier.PARA_LLEVAR)))));

        // 13000 (latte grande + shot + deslactosada) + 5000 (croissant)
        assertEquals(0, new BigDecimal("18000").compareTo(created.getTotal()));
        assertEquals(OrderStatus.RECIBIDO, created.getStatus());
        assertEquals(PaymentStatus.PENDIENTE, created.getPaymentStatus());

        OrderResponse reloaded = orderService.getOrder(created.getId());
        assertEquals(2, reloaded.getItems().size());
        assertEquals("Ana", reloaded.getCustomerName());
    }

    @Test
    void rechazaPedidoSinItems() {
        assertThrows(BusinessRuleException.class,
                () -> orderService.createOrder(new CreateOrderRequest("Ana", Collections.emptyList())));
    }

    @Test
    void rechazaNombreDeClienteVacio() {
        assertThrows(BusinessRuleException.class, () -> orderService.createOrder(new CreateOrderRequest("   ",
                Arrays.asList(new OrderItemRequest(CROISSANT, null, Collections.emptyList())))));
    }

    @Test
    void rechazaModificadorNoValidoParaElProducto() {
        assertThrows(BusinessRuleException.class, () -> orderService.createOrder(new CreateOrderRequest("Luis",
                Arrays.asList(new OrderItemRequest(CROISSANT, null, Arrays.asList(Modifier.SHOT_EXTRA))))));
    }

    @Test
    void rechazaTamanoIncompatible() {
        assertThrows(BusinessRuleException.class, () -> orderService.createOrder(new CreateOrderRequest("Luis",
                Arrays.asList(new OrderItemRequest(FRAPPE, Size.PEQUENO, Collections.emptyList())))));
    }

    @Test
    void rechazaProductoInexistente() {
        assertThrows(BusinessRuleException.class, () -> orderService.createOrder(new CreateOrderRequest("Luis",
                Arrays.asList(new OrderItemRequest(999L, null, Collections.emptyList())))));
    }

    @Test
    void recorreElFlujoCompletoCajaCocinaCaja() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);                         // caja cobra
        assertEquals(OrderStatus.EN_PREPARACION, orderService.take(id).getStatus());       // cocina toma
        assertEquals(OrderStatus.LISTO_PARA_RECOGER, orderService.markReady(id).getStatus()); // cocina: listo
        assertEquals(OrderStatus.ENTREGADO, orderService.deliver(id).getStatus());          // caja entrega
    }

    @Test
    void cocinaNoPuedeTomarUnPedidoSinPagar() {
        Long id = createSimpleOrder();
        assertThrows(BusinessRuleException.class, () -> orderService.take(id));
        assertEquals(OrderStatus.RECIBIDO, orderService.getOrder(id).getStatus());
    }

    @Test
    void rechazaSaltarseEstados() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);
        assertThrows(InvalidStatusTransitionException.class, () -> orderService.markReady(id));
        assertThrows(InvalidStatusTransitionException.class, () -> orderService.deliver(id));
    }

    @Test
    void rechazaDevolverEstados() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);
        orderService.take(id);
        assertThrows(InvalidStatusTransitionException.class, () -> orderService.take(id));
    }

    @Test
    void permiteCancelarEnRecibidoSinPagar() {
        Long id = createSimpleOrder();
        assertEquals(OrderStatus.CANCELADO, orderService.cancel(id).getStatus());
    }

    @Test
    void rechazaCancelarUnPedidoPagado() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);
        assertThrows(BusinessRuleException.class, () -> orderService.cancel(id));
        assertEquals(OrderStatus.RECIBIDO, orderService.getOrder(id).getStatus());
    }

    @Test
    void rechazaCancelarEnPreparacion() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);
        orderService.take(id);
        assertThrows(InvalidStatusTransitionException.class, () -> orderService.cancel(id));
    }

    @Test
    void registraElPago() {
        Long id = createSimpleOrder();
        assertTrue(orderService.registerPayment(id).isPaid());
    }

    @Test
    void elPagoNoVuelveAPendiente() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);
        assertThrows(BusinessRuleException.class, () -> orderService.updatePayment(id, PaymentStatus.PENDIENTE));
        assertThrows(BusinessRuleException.class, () -> orderService.registerPayment(id));
        assertTrue(orderService.getOrder(id).isPaid());
    }

    @Test
    void noRegistraPagoDePedidoCancelado() {
        Long id = createSimpleOrder();
        orderService.cancel(id);
        assertThrows(BusinessRuleException.class, () -> orderService.registerPayment(id));
    }

    @Test
    void cocinaSoloVePedidosPagados() {
        Long sinPagar = createSimpleOrder();
        Long pagado = createSimpleOrder();
        Long enPreparacion = createSimpleOrder();
        orderService.registerPayment(pagado);
        orderService.registerPayment(enPreparacion);
        orderService.take(enPreparacion);

        List<OrderResponse> kitchen = orderService.findKitchenOrders();
        assertTrue(kitchen.stream().noneMatch(o -> o.getId().equals(sinPagar)));
        assertTrue(kitchen.stream().anyMatch(o -> o.getId().equals(pagado)));
        assertTrue(kitchen.stream().anyMatch(o -> o.getId().equals(enPreparacion)));
    }

    @Test
    void lasBanderasDeAccionesSiguenLasReglas() {
        Long id = createSimpleOrder();
        OrderResponse nuevo = orderService.getOrder(id);
        assertTrue(nuevo.isCanPay());
        assertTrue(nuevo.isCancellable());
        assertFalse(nuevo.isCanTake());

        OrderResponse pagado = orderService.registerPayment(id);
        assertFalse(pagado.isCanPay());
        assertFalse(pagado.isCancellable());
        assertTrue(pagado.isCanTake());
    }

    @Test
    void consultaPedidosPorEstado() {
        Long id = createSimpleOrder();
        orderService.registerPayment(id);
        orderService.take(id);
        List<OrderResponse> inPreparation = orderService.findOrders(OrderStatus.EN_PREPARACION);
        assertTrue(inPreparation.stream().anyMatch(o -> o.getId().equals(id)));
        assertTrue(orderService.findOrders(OrderStatus.RECIBIDO).stream().noneMatch(o -> o.getId().equals(id)));
    }

    @Test
    void lanzaNotFoundSiElPedidoNoExiste() {
        assertThrows(NotFoundException.class, () -> orderService.getOrder(99999L));
    }

    private Long createSimpleOrder() {
        return orderService.createOrder(new CreateOrderRequest("Cliente prueba",
                Arrays.asList(new OrderItemRequest(CROISSANT, null, Collections.emptyList())))).getId();
    }
}
