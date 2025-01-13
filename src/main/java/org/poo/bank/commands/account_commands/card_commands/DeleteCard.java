package org.poo.bank.commands.account_commands.card_commands;

import org.poo.bank.account.Account;
import org.poo.bank.cards.Card;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.List;

public final class DeleteCard {
    private final List<User> users;

    public DeleteCard(final List<User> users) {
        this.users = users;
    }

    /**
     * Executa comanda de stergere a unui card asociat unui cont al unui utilizator.
     * Daca cardul e gasit, acesta e eliminat si se inregistreaza o tranzactie.
     *
     * @param command Comanda care contine detaliile cardului.
     */
    public void deleteCard(final CommandInput command) {
        User user = User.findByEmail(users, command.getEmail());
        if (user == null) {
            return;
        }

        for (Account account : user.getAccounts()) {
            Card card = account.getCardByNumber(command.getCardNumber());
            if (card != null) {
                account.removeCard(card);
                Transaction transaction = new Transaction(
                        command.getTimestamp(),
                        "The card has been destroyed",
                        account.getIban(),
                        null,
                        0,
                        account.getCurrency(),
                        null,
                        card.getCardNumber(),
                        user.getEmail(),
                        null,
                        null,
                        null,
                        null,
                        true,
                        null,
                        null,
                        "deleteCard"
                );

                user.addTransaction(transaction);
                return;
            }
        }
    }
}
