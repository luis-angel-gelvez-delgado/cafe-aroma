package com.cafearoma.pedidos.dto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/** Una opción (tamaño o modificador) válida para un producto, con su recargo. */
public final class OptionResponse {

    private final String code;
    private final String label;
    private final BigDecimal surcharge;
    private final String displayLabel;

    public OptionResponse(String code, String label, BigDecimal surcharge, String displayLabel) {
        this.code = code;
        this.label = label;
        this.surcharge = surcharge;
        this.displayLabel = displayLabel;
    }

    public static OptionResponse of(String code, String label, BigDecimal surcharge) {
        String display = surcharge.signum() > 0 ? label + " (+$" + formatPesos(surcharge) + ")" : label;
        return new OptionResponse(code, label, surcharge, display);
    }

    /** 1500 → "1.500" (pesos colombianos, sin decimales). */
    static String formatPesos(BigDecimal value) {
        return String.format(Locale.ROOT, "%,d", value.longValue()).replace(',', '.');
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OptionResponse)) return false;
        OptionResponse that = (OptionResponse) o;
        return Objects.equals(code, that.code) && Objects.equals(label, that.label)
                && Objects.equals(surcharge, that.surcharge) && Objects.equals(displayLabel, that.displayLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, label, surcharge, displayLabel);
    }
}
