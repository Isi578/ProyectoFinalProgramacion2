package com.hoteluq.application.loyalty;

import java.math.BigDecimal;

/**
 * Reglas de pago con puntos de HotelUQ.
 *
 * Cada punto cubre 50 COP del valor de una compra.
 * No representa dinero ingresado: los informes fiscales solo contabilizan
 * los cargos efectivos de la tarjeta virtual.
 */
public final class PointsPaymentPolicy {
    public static final BigDecimal COP_PER_POINT = new BigDecimal("50");

    private PointsPaymentPolicy() { }

    /** Calcula el valor cubierto por una cantidad positiva o nula de puntos. */
    public static BigDecimal discount(int points) {
        if (points < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos.");
        return COP_PER_POINT.multiply(BigDecimal.valueOf(points));
    }

    /** Impide pagar con puntos un valor superior al total de la operación. */
    public static BigDecimal remainder(BigDecimal total, int points) {
        if (total == null || total.signum() <= 0) {
            throw new IllegalArgumentException("El total debe ser mayor que cero.");
        }
        BigDecimal due = total.subtract(discount(points));
        if (due.signum() < 0) {
            throw new IllegalArgumentException("Los puntos superan el valor de la compra.");
        }
        return due;
    }
}