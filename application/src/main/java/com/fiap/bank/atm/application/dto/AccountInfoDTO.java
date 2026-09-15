package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contrato de leitura de uma conta, exposto pela camada de aplicação.
 * <p>
 * Implementado como Java Record para garantir imutabilidade e leveza.
 * Nenhuma referência à entidade de domínio {@code Account} é exposta aqui -
 * apenas tipos "neutros" (UUID, String, BigDecimal, boolean), como exige a
 * regra de isolamento físico entre as camadas.
 */
public record AccountInfoDTO(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        BigDecimal dailyWithdrawalLimit,
        BigDecimal totalWithdrawnToday,
        boolean blocked) {
}
