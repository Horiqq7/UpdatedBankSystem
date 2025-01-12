package org.poo.bank.commands.account_commands.card_commands;

import org.poo.bank.account.Account;
import org.poo.bank.cards.OneTimeCard;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;
import org.poo.utils.Utils;

import java.util.List;


public final class CreateOneTimeCard {
    private final List<User> users;

    public CreateOneTimeCard(final List<User> users) {
        this.users = users;
    }

    /**
     * Creeaza un card de unica folosinta.
     * Caut utilizatorul pe baza email-ului si contul pe baza IBAN-ului, generez un
     * numar de card si adaug cardul in contul utilizatorului. Se creeaza si o tranzactie.
     *
     * @param command Comanda care contine detalii despre utilizator, cont si timestamp.
     */
    public void createOneTimeCard(final CommandInput command) {
        User user = User.findByEmail(users, command.getEmail());
        if (user == null) {
            return;
        }

        Account account = user.getAccountByIBAN(command.getAccount());
        if (account == null) {
            return;
        }

        String cardNumber = Utils.generateCardNumber();
        OneTimeCard card = new OneTimeCard(cardNumber, "active");
        account.addCard(card);
        String description = "New card created";

        Transaction transaction = new Transaction(
                command.getTimestamp(),
                description,
                null,
                account.getIban(),
                0,
                account.getCurrency(),
                null,
                cardNumber,
                user.getEmail(),
                null,
                null,
                null,
                null,
                true,
                "createCard"
        );
        account.addTransaction(transaction);
        user.addTransaction(transaction);
    }
}
