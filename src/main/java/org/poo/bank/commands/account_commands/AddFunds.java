package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.List;

public final class AddFunds {
    private final List<User> users;

    public AddFunds(final List<User> users) {
        this.users = users;
    }

    /**
     * Adauga fonduri intr-un cont pe baza IBAN-ului specificat.
     * Dupa adaugarea fondurilor, creeaza o tranzactie si o adauga in istoricul contului.
     *
     * @param command Comanda care contine detalii despre contul IBAN si suma de adaugat.
     */
    public void addFunds(final CommandInput command) {
        String iban = command.getAccount();
        double amount = command.getAmount();

        for (User user : users) {
            Account account = user.getAccountByIBAN(iban);

            if (account != null) {
                account.addFunds(amount);
                Transaction transaction = new Transaction(
                        0,
                        "Funds added",
                        null,
                        iban,
                        amount,
                        account.getCurrency(),
                        "addFunds",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        "addFunds"
                );
                account.addTransaction(transaction);
                break;
            }
        }
    }
}
