package com.hoteluq.application.port;

import com.hoteluq.domain.model.VirtualCard;

import java.util.List;
import java.util.Optional;

public interface VirtualCardRepository {
    VirtualCard save(VirtualCard card);
    Optional<VirtualCard> findById(String cardId);
    List<VirtualCard> findByCustomerId(String customerId);
    List<VirtualCard> findAll();
}
