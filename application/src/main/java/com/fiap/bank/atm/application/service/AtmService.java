package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.application.exception.AccountBlockedException;
import com.fiap.bank.atm.application.exception.DailyLimitExceededException;
import com.fiap.bank.atm.application.exception.InsufficientFundsException;
import com.fiap.bank.atm.application.exception.InvalidOperationException;
import com.fiap.bank.atm.application.exception.InvalidPinException;
import com.fiap.bank.atm.application.exception.NotAuthenticatedException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Camada de aplicação (orquestração de casos de uso) do caixa eletrônico.
 * <p>
 * Regras seguidas à risca aqui:
 * <ul>
 * <li>Todo método público recebe e devolve apenas DTOs (records) ou tipos
 * "neutros" (UUID, BigDecimal, boolean) - nunca entidades de {@code domain}.</li>
 * <li>Toda busca no repositório é tratada via {@link Optional}, nunca
 * comparando contra {@code null}.</li>
 * <li>Exceptions de domínio são capturadas e traduzidas para exceptions
 * próprias da camada de aplicação, para que a apresentação nunca precise
 * importar {@code domain.exception}.</li>
 * </ul>
 */
public class AtmService {
    private final AccountRepository accountRepository;
    private Account currentAccount;

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new InvalidOperationException("Conta não encontrada."));

        try {
            account.authenticate(pin);
            currentAccount = account;
            return toAccountInfoDTO(account);
        } catch (RuntimeException e) {
            // Persiste tentativas falhas / bloqueio mesmo em caso de erro de autenticação
            accountRepository.save(account);
            throw translateDomainException(e);
        }
    }

    public void withdraw(BigDecimal amount) {
        ensureAuthenticated();
        try {
            currentAccount.withdraw(Money.of(amount));
        } catch (RuntimeException e) {
            throw translateDomainException(e);
        }
        accountRepository.save(currentAccount);
    }

    public void deposit(BigDecimal amount) {
        ensureAuthenticated();
        try {
            currentAccount.deposit(Money.of(amount));
        } catch (RuntimeException e) {
            throw translateDomainException(e);
        }
        accountRepository.save(currentAccount);
    }

    public void transfer(String targetAccountNumber, BigDecimal amount) {
        ensureAuthenticated();

        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new InvalidOperationException("Conta de destino não encontrada."));

        try {
            currentAccount.transfer(targetAccount, Money.of(amount));
        } catch (RuntimeException e) {
            throw translateDomainException(e);
        }

        accountRepository.save(currentAccount);
        accountRepository.save(targetAccount);
    }

    public BigDecimal getBalance() {
        ensureAuthenticated();
        return currentAccount.getBalance().getAmount();
    }

    public List<TransactionDTO> getStatement() {
        ensureAuthenticated();
        return currentAccount.getTransactions().stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retorna as {@code count} transações mais recentes da conta autenticada,
     * já ordenadas da mais nova para a mais antiga. Modernização funcional
     * (Fase 3): substitui o laço {@code for} que existia na apresentação por
     * uma pipeline de Streams.
     */
    public List<TransactionDTO> getLastTransactions(int count) {
        ensureAuthenticated();
        return currentAccount.getTransactions().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .limit(count)
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    public void logout() {
        currentAccount = null;
    }

    public Optional<AccountInfoDTO> getCurrentAccount() {
        return Optional.ofNullable(currentAccount).map(this::toAccountInfoDTO);
    }

    public boolean isAuthenticated() {
        return currentAccount != null;
    }

    private void ensureAuthenticated() {
        if (!isAuthenticated()) {
            throw new NotAuthenticatedException("Nenhum usuário está autenticado no momento.");
        }
    }

    private AccountInfoDTO toAccountInfoDTO(Account account) {
        return new AccountInfoDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance().getAmount(),
                account.getDailyWithdrawalLimit().getAmount(),
                account.getTotalWithdrawnToday().getAmount(),
                account.isBlocked());
    }

    private TransactionDTO toTransactionDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getTimestamp(),
                transaction.getType().getDescription(),
                transaction.getAmount().getAmount(),
                transaction.getDescription());
    }

    /**
     * Converte exceptions lançadas pelo domínio ({@code domain.exception})
     * para os equivalentes da camada de aplicação, mantendo a mensagem
     * original. Assim, nenhuma camada externa precisa conhecer os tipos de
     * exception do domínio.
     */
    private RuntimeException translateDomainException(RuntimeException e) {
        String message = e.getMessage();
        return switch (e) {
            case com.fiap.bank.atm.domain.exception.AccountBlockedException ignored ->
                    new AccountBlockedException(message);
            case com.fiap.bank.atm.domain.exception.InvalidPinException ignored ->
                    new InvalidPinException(message);
            case com.fiap.bank.atm.domain.exception.InsufficientFundsException ignored ->
                    new InsufficientFundsException(message);
            case com.fiap.bank.atm.domain.exception.DailyLimitExceededException ignored ->
                    new DailyLimitExceededException(message);
            case IllegalArgumentException ignored -> new InvalidOperationException(message);
            default -> e;
        };
    }
}
