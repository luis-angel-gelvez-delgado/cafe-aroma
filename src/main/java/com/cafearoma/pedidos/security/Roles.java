package com.cafearoma.pedidos.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Roles de la aplicación y la página de inicio de cada uno. */
public final class Roles {

    public static final String CAJA = "CAJA";
    public static final String COCINA = "COCINA";

    private Roles() {
    }

    /** Caja entra a Tomar pedido; cocina entra a Cocina. Sin sesión, al login. */
    public static String homePath(Authentication auth) {
        if (has(auth, CAJA)) {
            return "/pedido";
        }
        if (has(auth, COCINA)) {
            return "/cocina";
        }
        return "/login";
    }

    public static boolean has(Authentication auth, String role) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (("ROLE_" + role).equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
