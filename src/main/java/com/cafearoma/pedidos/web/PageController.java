package com.cafearoma.pedidos.web;

import com.cafearoma.pedidos.dto.ProductResponse;
import com.cafearoma.pedidos.security.Roles;
import com.cafearoma.pedidos.service.CatalogService;
import com.cafearoma.pedidos.service.NotFoundException;
import com.cafearoma.pedidos.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Páginas de la aplicación (Thymeleaf). Las de Pedidos (caja) y Cocina solo entregan el
 * esqueleto: live-board.js las llena consultando la API, que usa los mismos servicios.
 * Qué rol entra a cada página se define en SecurityConfig.
 */
@Controller
public class PageController {

    private final CatalogService catalogService;
    private final OrderService orderService;

    public PageController(CatalogService catalogService, OrderService orderService) {
        this.catalogService = catalogService;
        this.orderService = orderService;
    }

    /** Cada rol cae en su página: caja en Tomar pedido, cocina en Cocina. */
    @GetMapping("/")
    public String home(Authentication authentication) {
        return "redirect:" + Roles.homePath(authentication);
    }

    /** Login propio. Si ya hay sesión, manda a la página del rol. */
    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:" + Roles.homePath(authentication);
        }
        return "login";
    }

    /** Tomar pedido: menú y carrito. */
    @GetMapping("/pedido")
    public String takeOrder(@RequestParam(name = "nuevo", required = false) Long createdOrderId, Model model) {
        Map<String, List<ProductResponse>> menu = catalogService.findAll().stream()
                .collect(Collectors.groupingBy(ProductResponse::getCategoryLabel, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("menu", menu);
        if (createdOrderId != null) {
            try {
                model.addAttribute("createdOrder", orderService.getOrder(createdOrderId));
            } catch (NotFoundException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "pedido";
    }

    /** Pedidos (caja): cobrar, cancelar y entregar. */
    @GetMapping("/caja")
    public String cashier() {
        return "caja";
    }

    /** Cocina: tomar y marcar listo. */
    @GetMapping("/cocina")
    public String kitchen() {
        return "cocina";
    }

    /** Historial: entregados y cancelados. */
    @GetMapping("/historial")
    public String history(Model model) {
        model.addAttribute("closedOrders", orderService.findClosedOrders());
        return "historial";
    }
}
