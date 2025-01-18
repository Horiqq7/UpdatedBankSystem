package org.poo.bank.commands.print_commands;

import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public final class PrintTransactions {
    private final List<User> users;

    public PrintTransactions(final List<User> users) {
        this.users = users;
    }

    /**
     * Metoda care afiseaza tranzactiile unui utilizator pe baza unui email.
     * @param command Comanda care contine datele necesare.
     * @return O lista de harti care reprezinta tranzactiile utilizatorului.
     */
    public List<Map<String, Object>> printTransactions(final CommandInput command) {
        String email = command.getEmail();
        User user = User.findByEmail(users, email);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + email);
        }

        List<Transaction> transactions = user.getTransactions();
        List<Map<String, Object>> outputTransactions = new ArrayList<>();

        transactions.sort(Comparator.comparingInt(Transaction::getTimestamp));

        for (Transaction transaction : transactions) {
            outputTransactions.add(transaction.toMap());
        }
        return outputTransactions;
    }
}
