package com.cafearoma.pedidos.web;

import com.cafearoma.pedidos.security.Roles;
import com.cafearoma.pedidos.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Páginas de la aplicación (Thymeleaf). Las de Pedidos (caja) y Cocina solo entregan el
 * esqueleto: live-board.js las llena consultando la API, que usa los mismos servicios.
 * Qué rol entra a cada página se define en SecurityConfig.
 */
@Controller
public class PageController {

    private final OrderService orderService;

    public PageController(OrderService orderService) {
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

    /** Tomar pedido: kiosko paso a paso, con resumen y cobro al registrar (order-form.js). */
    @GetMapping("/pedido")
    public String takeOrder() {
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
