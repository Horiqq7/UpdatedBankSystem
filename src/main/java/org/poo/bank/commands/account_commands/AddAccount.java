package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;
import org.poo.utils.Utils;

import java.util.List;

public final class AddAccount {
    private final List<User> users;

    public AddAccount(final List<User> users) {
        this.users = users;
    }

    /**
     * Adauga un cont nou unui utilizator existent. Creaza un cont folosind un IBAN generat
     * automat, seteaza detaliile contului si inregistreaza tranzactia de creare a contului.
     *
     * @param command Comanda care contine informatii despre
     * utilizator si detalii pentru contul nou.
     */
    public void addAccount(final CommandInput command) {
        User user = User.findByEmail(users, command.getEmail());

        if (user == null) {
            System.out.println("User not found: " + command.getEmail());
            return;
        }

        String iban = Utils.generateIBAN();

        Account account = new Account.AccountBuilder(iban, command.getCurrency(),
                command.getAccountType())
                .balance(0)
                .minimumBalance(0)
                .interestRate(command.getInterestRate())
                .build();


        user.addAccount(account);

        Transaction transaction = new Transaction(
                command.getTimestamp(),
                "New account created",
                null,
                iban,
                0,
                command.getCurrency(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "addAccount"
        );

        user.addTransaction(transaction);
        account.addTransaction(transaction);
    }
}
