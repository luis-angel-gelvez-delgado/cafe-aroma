package com.cafearoma.pedidos.security;

import com.cafearoma.pedidos.dto.CreateOrderRequest;
import com.cafearoma.pedidos.dto.OrderItemRequest;
import com.cafearoma.pedidos.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba que cada rol solo entra a lo suyo, en las páginas y en la API.
 * Usa los usuarios de application-test.properties (caja-test / cocina-test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    private static final long CROISSANT = 8L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderService orderService;

    // --- Login ---

    @Test
    void sinSesionLasPaginasMandanAlLogin() throws Exception {
        mvc.perform(get("/caja")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void sinSesionLaApiResponde401() throws Exception {
        mvc.perform(get("/api/orders/active")).andExpect(status().isUnauthorized());
    }

    @Test
    void cajaCaeEnTomarPedido() throws Exception {
        mvc.perform(formLogin("/login").user("caja").password("caja-test"))
                .andExpect(redirectedUrl("/pedido"));
    }

    @Test
    void cocinaCaeEnCocina() throws Exception {
        mvc.perform(formLogin("/login").user("cocina").password("cocina-test"))
                .andExpect(redirectedUrl("/cocina"));
    }

    @Test
    void claveIncorrectaVuelveAlLoginConError() throws Exception {
        mvc.perform(formLogin("/login").user("caja").password("otra"))
                .andExpect(redirectedUrl("/login?error"));
    }

    // --- Páginas por rol ---

    @Test
    @WithMockUser(roles = "COCINA")
    void cocinaQueEntraACajaVaASuPagina() throws Exception {
        mvc.perform(get("/caja")).andExpect(redirectedUrl("/cocina"));
        mvc.perform(get("/pedido")).andExpect(redirectedUrl("/cocina"));
    }

    @Test
    @WithMockUser(roles = "CAJA")
    void cajaQueEntraACocinaVaASuPagina() throws Exception {
        mvc.perform(get("/cocina")).andExpect(redirectedUrl("/pedido"));
    }

    @Test
    @WithMockUser(roles = "COCINA")
    void cocinaPuedeVerElHistorial() throws Exception {
        mvc.perform(get("/historial")).andExpect(status().isOk());
    }

    // --- API por rol ---

    @Test
    @WithMockUser(roles = "COCINA")
    void cocinaNoPuedeCrearNiCobrar() throws Exception {
        Long id = createOrder();
        mvc.perform(post("/api/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Ana\",\"items\":[{\"productId\":8}]}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/" + id + "/payment").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentStatus\":\"PAGADO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/active")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJA")
    void cajaNoPuedeHacerLosPasosDeCocina() throws Exception {
        Long id = createOrder();
        orderService.registerPayment(id);
        mvc.perform(patch("/api/orders/" + id + "/take").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/" + id + "/ready").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/kitchen")).andExpect(status().isForbidden());
    }

    @Test
    void cajaCobraYCocinaToma() throws Exception {
        Long id = createOrder();
        mvc.perform(patch("/api/orders/" + id + "/payment").with(httpBasic("caja", "caja-test"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentStatus\":\"PAGADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/orders/" + id + "/take").with(httpBasic("cocina", "cocina-test")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CAJA")
    void elNavegadorSinTokenCsrfRecibe403() throws Exception {
        Long id = createOrder();
        mvc.perform(patch("/api/orders/" + id + "/cancel")).andExpect(status().isForbidden());
    }

    @Test
    void conUsuarioYClaveLaApiFuncionaSinCsrf() throws Exception {
        mvc.perform(get("/api/orders/active").with(httpBasic("caja", "caja-test"))).andExpect(status().isOk());
        Long id = createOrder();
        mvc.perform(patch("/api/orders/" + id + "/cancel").with(httpBasic("caja", "caja-test")))
                .andExpect(status().isOk());
    }

    @Test
    void conClaveIncorrectaLaApiResponde401() throws Exception {
        mvc.perform(get("/api/orders/active").with(httpBasic("caja", "mala"))).andExpect(status().isUnauthorized());
    }

    private Long createOrder() {
        return orderService.createOrder(new CreateOrderRequest("Cliente prueba",
                Arrays.asList(new OrderItemRequest(CROISSANT, null, Collections.emptyList())))).getId();
    }
}
