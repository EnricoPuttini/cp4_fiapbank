package com.fiap.bank.atm.application.exception;

/**
 * Cobre o caso em que uma operação sensível é chamada sem um usuário
 * autenticado (equivalente ao antigo {@link IllegalStateException}).
 */
public class NotAuthenticatedException extends AtmApplicationException {
    public NotAuthenticatedException(String message) {
        super(message);
    }
}
