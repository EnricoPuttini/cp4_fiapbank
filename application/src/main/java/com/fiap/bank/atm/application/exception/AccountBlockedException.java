package com.fiap.bank.atm.application.exception;

/** Espelha a violação de domínio quando a conta está bloqueada. */
public class AccountBlockedException extends AtmApplicationException {
    public AccountBlockedException(String message) {
        super(message);
    }
}
