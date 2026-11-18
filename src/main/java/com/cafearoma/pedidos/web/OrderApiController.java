package com.cafearoma.pedidos.web;

import com.cafearoma.pedidos.dto.CreateOrderRequest;
import com.cafearoma.pedidos.dto.OrderResponse;
import com.cafearoma.pedidos.dto.UpdatePaymentRequest;
import com.cafearoma.pedidos.model.OrderStatus;
import com.cafearoma.pedidos.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** API REST de pedidos. No tiene reglas de negocio: solo delega en OrderService. */
@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** POST /api/orders → 201 Created con el pedido y su total calculado. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId())).body(created);
    }

    /** GET /api/orders?status=RECIBIDO → pedidos filtrados por estado (sin filtro: todos). */
    @GetMapping
    public List<OrderResponse> list(@RequestParam(name = "status", required = false) OrderStatus status) {
        return orderService.findOrders(status);
    }

    /** GET /api/orders/active → pedidos activos (Recibido, En preparación, Listo para recoger), para caja. */
    @GetMapping("/active")
    public List<OrderResponse> active() {
        return orderService.findActiveOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable("id") Long id) {
        return orderService.getOrder(id);
    }

    // Acciones de caja

    /** PATCH /api/orders/{id}/payment {"paymentStatus":"PAGADO"} → registra el pago (no se puede revertir). */
    @PatchMapping("/{id}/payment")
    public OrderResponse updatePayment(@PathVariable("id") Long id, @RequestBody UpdatePaymentRequest request) {
        return orderService.updatePayment(id, request.getPaymentStatus());
    }

    /** PATCH /api/orders/{id}/cancel → solo en Recibido y sin pagar (409 / 400). */
    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable("id") Long id) {
        return orderService.cancel(id);
    }

    /** PATCH /api/orders/{id}/deliver → Listo para recoger → Entregado. */
    @PatchMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable("id") Long id) {
        return orderService.deliver(id);
    }

    // Acciones de cocina

    /** GET /api/orders/kitchen → Recibidos ya pagados y En preparación. */
    @GetMapping("/kitchen")
    public List<OrderResponse> kitchen() {
        return orderService.findKitchenOrders();
    }

    /** PATCH /api/orders/{id}/take → Recibido → En preparación (solo si está pagado). */
    @PatchMapping("/{id}/take")
    public OrderResponse take(@PathVariable("id") Long id) {
        return orderService.take(id);
    }

    /** PATCH /api/orders/{id}/ready → En preparación → Listo para recoger. */
    @PatchMapping("/{id}/ready")
    public OrderResponse ready(@PathVariable("id") Long id) {
        return orderService.markReady(id);
    }
}
