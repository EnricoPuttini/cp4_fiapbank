package com.fiap.bank.atm.application.exception;

/** Espelha a violação de domínio quando a senha informada é inválida. */
public class InvalidPinException extends AtmApplicationException {
    public InvalidPinException(String message) {
        super(message);
    }
}
