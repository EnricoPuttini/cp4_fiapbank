package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.BaseEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato genérico de repositório do domínio.
 * <p>
 * O limite superior {@code T extends BaseEntity} garante, em tempo de
 * compilação, que só entidades legítimas do domínio (com identidade própria)
 * possam ser persistidas através desta abstração.
 * <p>
 * Todas as buscas retornam {@link Optional}, banindo o uso de {@code null}
 * como sinalizador de "não encontrado".
 *
 * @param <T> tipo da entidade de domínio gerenciada por este repositório
 */
public interface ATMRepository<T extends BaseEntity> {

    Optional<T> findById(UUID id);

    void save(T entity);
}
