package com.fiap.bank.atm.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class BaseEntity {
    private final UUID id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BaseEntity(UUID id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Construtor de reconstituição, usado exclusivamente pela camada de
     * infraestrutura para reidratar uma entidade a partir de dados já
     * persistidos, preservando os timestamps originais.
     */
    protected BaseEntity(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
