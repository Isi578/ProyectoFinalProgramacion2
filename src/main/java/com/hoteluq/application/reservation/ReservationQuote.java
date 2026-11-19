package com.hoteluq.application.reservation;

import java.math.BigDecimal;

/** Cotización calculada por la misma lógica que confirma la reserva. */
public record ReservationQuote(BigDecimal subtotal, BigDecimal afterPromotion,
                               int pointsToSpend, BigDecimal pointsValue,
                               BigDecimal cardDue) { }
