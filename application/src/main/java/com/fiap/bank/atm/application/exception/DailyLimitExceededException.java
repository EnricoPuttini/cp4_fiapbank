package com.fiap.bank.atm.application.exception;

/** Espelha a violação de domínio quando o limite diário de saque é excedido. */
public class DailyLimitExceededException extends AtmApplicationException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
