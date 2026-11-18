package com.cafearoma.pedidos.web;

import com.cafearoma.pedidos.dto.OrderResponse;
import com.cafearoma.pedidos.dto.ProductResponse;
import com.cafearoma.pedidos.model.OrderStatus;
import com.cafearoma.pedidos.service.BusinessRuleException;
import com.cafearoma.pedidos.service.CatalogService;
import com.cafearoma.pedidos.service.NotFoundException;
import com.cafearoma.pedidos.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Pantalla del mostrador (Thymeleaf). Usa los mismos servicios que la API REST. */
@Controller
public class CounterController {

    private final CatalogService catalogService;
    private final OrderService orderService;

    public CounterController(CatalogService catalogService, OrderService orderService) {
        this.catalogService = catalogService;
        this.orderService = orderService;
    }

    @GetMapping("/")
    public String counter(@RequestParam(name = "nuevo", required = false) Long createdOrderId, Model model) {
        Map<String, List<ProductResponse>> menu = catalogService.findAll().stream()
                .collect(Collectors.groupingBy(ProductResponse::getCategoryLabel, LinkedHashMap::new, Collectors.toList()));

        Map<OrderStatus, List<OrderResponse>> board = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.activeStatuses()) {
            board.put(status, new ArrayList<>());
        }
        for (OrderResponse order : orderService.findActiveOrders()) {
            board.get(order.getStatus()).add(order);
        }

        model.addAttribute("menu", menu);
        model.addAttribute("board", board);
        model.addAttribute("closedOrders", orderService.findClosedOrders());
        if (createdOrderId != null) {
            try {
                model.addAttribute("createdOrder", orderService.getOrder(createdOrderId));
            } catch (NotFoundException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "counter";
    }

    // Botones de la pantalla. Cada uno llama a la misma regla del servicio que usa la API.

    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return run(redirect, () -> orderService.registerPayment(id), "quedó pagado");
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return run(redirect, () -> orderService.cancel(id), "fue cancelado");
    }

    @PostMapping("/orders/{id}/take")
    public String take(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return run(redirect, () -> orderService.take(id), "pasó a «En preparación»");
    }

    @PostMapping("/orders/{id}/ready")
    public String ready(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return run(redirect, () -> orderService.markReady(id), "está «Listo para recoger»");
    }

    @PostMapping("/orders/{id}/deliver")
    public String deliver(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return run(redirect, () -> orderService.deliver(id), "fue entregado");
    }

    private String run(RedirectAttributes redirect, Supplier<OrderResponse> action, String result) {
        try {
            OrderResponse order = action.get();
            redirect.addFlashAttribute("success", "Pedido " + order.getOrderNumber() + " " + result + ".");
        } catch (BusinessRuleException | NotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }
}
