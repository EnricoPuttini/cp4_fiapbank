package com.fiap.bank.atm.application.exception;

/**
 * Superclasse de todas as exceptions de negócio expostas pela camada de
 * aplicação. Permite que a camada de apresentação, quando quiser, capture um
 * único tipo genérico sem nunca precisar importar exceptions do domain.
 */
public abstract class AtmApplicationException extends RuntimeException {
    protected AtmApplicationException(String message) {
        super(message);
    }
}
