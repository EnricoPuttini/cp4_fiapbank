package com.fiap.bank.atm.infrastructure;

import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl;
import com.fiap.bank.atm.infrastructure.persistence.SQLiteConnectionFactory;
import com.fiap.bank.atm.presentation.AtmFrame;
import javax.swing.SwingUtilities;

/**
 * Raiz de composição da aplicação (Composition Root).
 * <p>
 * É a única classe do projeto autorizada a conhecer as quatro camadas ao
 * mesmo tempo: monta a infraestrutura concreta (JDBC/SQLite), injeta essa
 * implementação na camada de aplicação via a abstração {@link AccountRepository}
 * do domínio, e por fim entrega o serviço já pronto para a camada de
 * apresentação - que nunca enxerga domain nem infrastructure diretamente.
 */
public class AtmApplication {
    public static void main(String[] args) {
        // Infraestrutura: persistência real em SQLite via JDBC puro (sem ORM)
        SQLiteConnectionFactory connectionFactory = new SQLiteConnectionFactory();
        AccountRepository accountRepository = new AccountRepositoryJdbcImpl(connectionFactory);

        // Aplicação: orquestra os casos de uso sobre a abstração de domínio
        AtmService atmService = new AtmService(accountRepository);

        // Apresentação: sobe a tela Swing na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}
