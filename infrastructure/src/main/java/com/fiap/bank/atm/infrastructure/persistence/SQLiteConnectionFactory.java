package com.fiap.bank.atm.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Responsável por abrir/fechar conexões com o arquivo de banco de dados
 * SQLite e por garantir que o schema necessário exista antes do primeiro uso.
 * <p>
 * Toda a persistência deste projeto usa exclusivamente a API nativa JDBC -
 * nenhum ORM (Hibernate/JPA/Spring Data) é utilizado, conforme exigido.
 */
public final class SQLiteConnectionFactory {

    private static final String DB_FILE = "fiapbank_atm.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private static boolean schemaInitialized = false;

    /** Abre uma nova conexão com o banco, garantindo que o schema já exista. */
    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL);
            ensureSchema(connection);
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível conectar ao banco de dados SQLite.", e);
        }
    }

    /** Encerra a conexão silenciosamente, registrando eventuais falhas. */
    public void close(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Falha ao fechar conexão com o banco: " + e.getMessage());
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        if (schemaInitialized) {
            return;
        }

        String createAccounts = """
                CREATE TABLE IF NOT EXISTS accounts (
                    id TEXT PRIMARY KEY,
                    account_number TEXT UNIQUE NOT NULL,
                    pin TEXT NOT NULL,
                    balance TEXT NOT NULL,
                    daily_withdrawal_limit TEXT NOT NULL,
                    total_withdrawn_today TEXT NOT NULL,
                    blocked INTEGER NOT NULL,
                    failed_attempts INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                );
                """;

        String createTransactions = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT PRIMARY KEY,
                    account_id TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    description TEXT,
                    FOREIGN KEY (account_id) REFERENCES accounts (id)
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(createAccounts);
            statement.execute(createTransactions);
        }

        schemaInitialized = true;
    }
}
