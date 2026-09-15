package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.Account;
import java.util.Optional;

/**
 * Especialização de {@link ATMRepository} para a entidade {@link Account}.
 * <p>
 * A busca por número de conta - operação de consulta mais usada pelo caixa
 * eletrônico - também é obrigada a devolver {@link Optional}, eliminando
 * definitivamente o retorno de {@code null} literal.
 */
public interface AccountRepository extends ATMRepository<Account> {

    Optional<Account> findByAccountNumber(String accountNumber);
}
