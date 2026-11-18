package com.cafearoma.pedidos.security;

import com.cafearoma.pedidos.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Login y permisos por rol (Spring Security 5.7).
 * <ul>
 *   <li>CAJA: Tomar pedido, Pedidos (caja) e Historial; en la API crea, cobra, cancela y entrega.</li>
 *   <li>COCINA: Cocina e Historial (solo lectura); en la API toma y marca listo.</li>
 * </ul>
 * El permiso se valida en el servidor por cada ruta, no solo escondiendo opciones del menú.
 */
@Configuration
public class SecurityConfig {

    private static final AntPathRequestMatcher API = new AntPathRequestMatcher("/api/**");

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(auth -> auth
                // Público: login, estilos, scripts y la página de error
                .antMatchers("/login", "/css/**", "/js/**", "/error").permitAll()

                // Páginas
                .antMatchers("/pedido", "/caja").hasRole(Roles.CAJA)
                .antMatchers("/cocina").hasRole(Roles.COCINA)
                .antMatchers("/historial").hasAnyRole(Roles.CAJA, Roles.COCINA)

                // API de caja
                .antMatchers(HttpMethod.POST, "/api/orders").hasRole(Roles.CAJA)
                .antMatchers(HttpMethod.GET, "/api/orders/active").hasRole(Roles.CAJA)
                .antMatchers(HttpMethod.PATCH, "/api/orders/*/payment", "/api/orders/*/cancel",
                        "/api/orders/*/deliver").hasRole(Roles.CAJA)

                // API de cocina
                .antMatchers(HttpMethod.GET, "/api/orders/kitchen").hasRole(Roles.COCINA)
                .antMatchers(HttpMethod.PATCH, "/api/orders/*/take", "/api/orders/*/ready").hasRole(Roles.COCINA)

                // Consultas (menú, listado y detalle): cualquiera de los dos roles
                .antMatchers(HttpMethod.GET, "/api/**").hasAnyRole(Roles.CAJA, Roles.COCINA)

                // Lo demás (incluida "/") exige sesión; cualquier otra ruta de la API queda cerrada
                .antMatchers("/api/**").denyAll()
                .anyRequest().authenticated())

            // Página de login propia; cada rol cae en su página
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) ->
                        response.sendRedirect(request.getContextPath() + Roles.homePath(authentication)))
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?salir").permitAll())

            // La API también acepta usuario y contraseña (curl.exe -u caja:clave ...)
            .httpBasic(basic -> basic.authenticationEntryPoint(apiUnauthorized()))

            // CSRF: lo exigen las peticiones del navegador (sesión). Las que traen Authorization
            // (curl con -u) no lo necesitan, porque el navegador nunca envía ese encabezado solo.
            .csrf(csrf -> csrf.requireCsrfProtectionMatcher(browserRequestsOnly()))

            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(notLoggedIn())
                .accessDeniedHandler(accessDenied()));
        return http.build();
    }

    /** Sin sesión: la API responde 401 en JSON y las páginas mandan al login. */
    private AuthenticationEntryPoint notLoggedIn() {
        AuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");
        AuthenticationEntryPoint api = apiUnauthorized();
        return (request, response, e) -> {
            if (API.matches(request)) {
                api.commence(request, response, e);
            } else {
                login.commence(request, response, e);
            }
        };
    }

    /** API sin sesión → 401 en JSON (sin redirigir al login ni pedir la ventana de usuario del navegador). */
    private AuthenticationEntryPoint apiUnauthorized() {
        return (request, response, e) -> writeJson(response, HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesión para usar la API.");
    }

    /**
     * Sesión iniciada pero sin permiso: la API responde 403 en JSON;
     * en las páginas se manda a la persona a la página de su rol.
     */
    private AccessDeniedHandler accessDenied() {
        return (request, response, e) -> {
            if (API.matches(request)) {
                String message = e instanceof CsrfException
                        ? "La sesión expiró o falta el token CSRF: recarga la página."
                        : "Tu rol no tiene permiso para esta acción.";
                writeJson(response, HttpStatus.FORBIDDEN, message);
            } else {
                String home = Roles.homePath(SecurityContextHolder.getContext().getAuthentication());
                response.sendRedirect(request.getContextPath() + home);
            }
        };
    }

    private static RequestMatcher browserRequestsOnly() {
        return request -> CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
                && request.getHeader("Authorization") == null;
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(status.value(), message));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Usuarios "caja" y "cocina" en memoria; las contraseñas vienen de variables de entorno y se cifran con BCrypt. */
    @Bean
    public UserDetailsService users(PasswordEncoder encoder,
                                    @Value("${app.security.caja-password}") String cajaPassword,
                                    @Value("${app.security.cocina-password}") String cocinaPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername("caja").password(encoder.encode(required(cajaPassword, "CAJA_PASSWORD")))
                        .roles(Roles.CAJA).build(),
                User.withUsername("cocina").password(encoder.encode(required(cocinaPassword, "COCINA_PASSWORD")))
                        .roles(Roles.COCINA).build());
    }

    private static String required(String value, String variable) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Falta la variable de entorno " + variable + " (revisa el archivo .env).");
        }
        return value;
    }
}
