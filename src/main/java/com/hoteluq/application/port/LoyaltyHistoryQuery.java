package com.hoteluq.application.port;

import com.hoteluq.domain.loyalty.LoyaltyEntry;
import java.util.List;

/**
 * LoyaltyHistoryQuery (consulta del historial de fidelización).
 *
 * Permite consultar los movimientos de puntos
 * sin modificar el saldo del cliente.
 */
public interface LoyaltyHistoryQuery {

    /**
     * findEntries (buscar movimientos).
     *
     * customerId: identificador del cliente.
     *
     * Devuelve los movimientos de puntos
     * registrados para ese cliente.
     */
    List<LoyaltyEntry> findEntries(String customerId);
}