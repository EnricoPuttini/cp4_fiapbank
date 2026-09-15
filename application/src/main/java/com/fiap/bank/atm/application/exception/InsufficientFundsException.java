package com.fiap.bank.atm.application.exception;

/** Espelha a violação de domínio quando não há saldo suficiente. */
public class InsufficientFundsException extends AtmApplicationException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
