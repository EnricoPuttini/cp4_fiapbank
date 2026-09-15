package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrato de leitura de uma transação, exposto pela camada de aplicação.
 * <p>
 * {@code type} já vem como a descrição textual amigável (ex.: "Saque",
 * "Depósito"), então a camada de apresentação não precisa conhecer o enum de
 * domínio {@code TransactionType}.
 */
public record TransactionDTO(
        UUID id,
        LocalDateTime timestamp,
        String type,
        BigDecimal amount,
        String description) {
}
