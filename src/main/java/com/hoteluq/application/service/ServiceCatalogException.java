package com.hoteluq.application.service;

/** Domain exception for invalid additional-service administration. */
public final class ServiceCatalogException extends RuntimeException {
    public ServiceCatalogException(String message) { super(message); }
}