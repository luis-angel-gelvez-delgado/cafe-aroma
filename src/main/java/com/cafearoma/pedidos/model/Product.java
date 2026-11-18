package com.cafearoma.pedidos.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import java.math.BigDecimal;

/** Producto del menú. Se carga desde data.sql. */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    /** Precio de la opción más pequeña disponible (ver Category#sizeSurcharge). */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    protected Product() {
        // requerido por JPA
    }

    public Product(String name, Category category, BigDecimal basePrice) {
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
