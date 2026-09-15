package com.fiap.bank.atm.application.exception;

/**
 * Cobre validações de aplicação que no código legado eram sinalizadas com
 * {@link IllegalArgumentException} (ex.: conta não encontrada, valor de
 * operação inválido, transferência para a própria conta).
 */
public class InvalidOperationException extends AtmApplicationException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
