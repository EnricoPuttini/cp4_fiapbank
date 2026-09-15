package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação concreta de {@link AccountRepository} usando exclusivamente
 * a API nativa JDBC contra um arquivo SQLite.
 * <p>
 * Nenhum ORM é utilizado. Toda montagem de SQL usa {@link PreparedStatement}
 * com parâmetros via métodos {@code set*}, nunca concatenação de Strings,
 * eliminando o risco de SQL Injection.
 */
public class AccountRepositoryJdbcImpl implements AccountRepository {

    private final SQLiteConnectionFactory connectionFactory;

    public AccountRepositoryJdbcImpl(SQLiteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        seedIfEmpty();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs, conn)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar conta por id.", e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = connectionFactory.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs, conn)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar conta por número.", e);
        }
    }

    @Override
    public void save(Account account) {
        String upsert = """
                INSERT INTO accounts
                    (id, account_number, pin, balance, daily_withdrawal_limit, total_withdrawn_today,
                     blocked, failed_attempts, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    balance = excluded.balance,
                    total_withdrawn_today = excluded.total_withdrawn_today,
                    blocked = excluded.blocked,
                    failed_attempts = excluded.failed_attempts,
                    updated_at = excluded.updated_at
                """;

        try (Connection conn = connectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, account.getId().toString());
                ps.setString(2, account.getAccountNumber());
                ps.setString(3, account.getPin());
                ps.setString(4, account.getBalance().getAmount().toPlainString());
                ps.setString(5, account.getDailyWithdrawalLimit().getAmount().toPlainString());
                ps.setString(6, account.getTotalWithdrawnToday().getAmount().toPlainString());
                ps.setInt(7, account.isBlocked() ? 1 : 0);
                ps.setInt(8, account.getFailedAttempts());
                ps.setString(9, account.getCreatedAt().toString());
                ps.setString(10, account.getUpdatedAt().toString());
                ps.executeUpdate();
            }

            persistTransactions(conn, account);
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar conta.", e);
        }
    }

    /**
     * Insere as transações da conta que ainda não estão persistidas.
     * Usa {@code INSERT OR IGNORE} com o id da transação como chave primária,
     * o que torna a operação idempotente sem a necessidade de rastrear
     * manualmente quais transações já foram salvas anteriormente.
     */
    private void persistTransactions(Connection conn, Account account) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO transactions (id, account_id, timestamp, type, amount, description)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Transaction tx : account.getTransactions()) {
                ps.setString(1, tx.getId().toString());
                ps.setString(2, account.getId().toString());
                ps.setString(3, tx.getTimestamp().toString());
                ps.setString(4, tx.getType().name());
                ps.setString(5, tx.getAmount().getAmount().toPlainString());
                ps.setString(6, tx.getDescription());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Account mapAccount(ResultSet rs, Connection conn) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String accountNumber = rs.getString("account_number");
        String pin = rs.getString("pin");
        Money balance = Money.of(new BigDecimal(rs.getString("balance")));
        Money dailyLimit = Money.of(new BigDecimal(rs.getString("daily_withdrawal_limit")));
        Money totalWithdrawnToday = Money.of(new BigDecimal(rs.getString("total_withdrawn_today")));
        boolean blocked = rs.getInt("blocked") == 1;
        int failedAttempts = rs.getInt("failed_attempts");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));

        List<Transaction> transactions = findTransactionsByAccountId(conn, id);

        return Account.reconstruct(id, createdAt, updatedAt, accountNumber, pin, balance, dailyLimit,
                totalWithdrawnToday, blocked, failedAttempts, transactions);
    }

    private List<Transaction> findTransactionsByAccountId(Connection conn, UUID accountId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY timestamp ASC";
        List<Transaction> transactions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID txId = UUID.fromString(rs.getString("id"));
                    LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"));
                    TransactionType type = TransactionType.valueOf(rs.getString("type"));
                    Money amount = Money.of(new BigDecimal(rs.getString("amount")));
                    String description = rs.getString("description");
                    transactions.add(new Transaction(txId, timestamp, type, amount, description));
                }
            }
        }
        return transactions;
    }

    /** Popula o banco com as contas de demonstração apenas na primeira execução. */
    private void seedIfEmpty() {
        try (Connection conn = connectionFactory.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) AS total FROM accounts")) {
            if (rs.next() && rs.getInt("total") > 0) {
                return; // já existem dados de execuções anteriores - não sobrescrever
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao verificar dados iniciais do banco.", e);
        }
        seedInitialAccounts();
    }

    private void seedInitialAccounts() {
        Account acc1 = new Account(UUID.randomUUID(), "12345", "1234", Money.of(5000.00), Money.of(1500.00));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(3),
                TransactionType.DEPOSIT, Money.of(2000.00), "Depósito em dinheiro"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_IN, Money.of(500.00), "Transf. de Conta 67890"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(1),
                TransactionType.WITHDRAWAL, Money.of(100.00), "Saque eletrônico"));
        save(acc1);

        Account acc2 = new Account(UUID.randomUUID(), "67890", "5678", Money.of(1200.00), Money.of(1000.00));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(5),
                TransactionType.DEPOSIT, Money.of(1500.00), "Depósito inicial"));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_OUT, Money.of(500.00), "Transf. para Conta 12345"));
        save(acc2);

        Account acc3 = new Account(UUID.randomUUID(), "99999", "9999", Money.of(50.00), Money.of(500.00));
        acc3.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(10),
                TransactionType.DEPOSIT, Money.of(50.00), "Abertura de conta"));
        save(acc3);
    }
}
