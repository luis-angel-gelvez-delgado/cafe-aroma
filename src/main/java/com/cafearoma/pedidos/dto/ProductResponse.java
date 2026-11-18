package com.cafearoma.pedidos.dto;

import com.cafearoma.pedidos.model.Category;
import com.cafearoma.pedidos.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Producto del menú con SOLO las opciones válidas para su categoría. */
public final class ProductResponse {

    private final Long id;
    private final String name;
    private final Category category;
    private final String categoryLabel;
    private final BigDecimal basePrice;
    private final List<OptionResponse> sizes;
    private final List<OptionResponse> modifiers;

    public ProductResponse(Long id, String name, Category category, String categoryLabel,
                           BigDecimal basePrice, List<OptionResponse> sizes, List<OptionResponse> modifiers) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.basePrice = basePrice;
        this.sizes = sizes;
        this.modifiers = modifiers;
    }

    public static ProductResponse from(Product product) {
        Category category = product.getCategory();
        List<OptionResponse> sizes = category.getAllowedSizes().stream()
                .map(s -> OptionResponse.of(s.name(), s.getLabel(), category.sizeSurcharge(s)))
                .collect(Collectors.toList());
        List<OptionResponse> modifiers = category.getAllowedModifiers().stream()
                .map(m -> OptionResponse.of(m.name(), m.getLabel(), m.getSurcharge()))
                .collect(Collectors.toList());
        return new ProductResponse(product.getId(), product.getName(), category, category.getLabel(),
                product.getBasePrice(), sizes, modifiers);
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

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public List<OptionResponse> getSizes() {
        return sizes;
    }

    public List<OptionResponse> getModifiers() {
        return modifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductResponse)) return false;
        ProductResponse that = (ProductResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && category == that.category
                && Objects.equals(categoryLabel, that.categoryLabel) && Objects.equals(basePrice, that.basePrice)
                && Objects.equals(sizes, that.sizes) && Objects.equals(modifiers, that.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, category, categoryLabel, basePrice, sizes, modifiers);
    }
}
