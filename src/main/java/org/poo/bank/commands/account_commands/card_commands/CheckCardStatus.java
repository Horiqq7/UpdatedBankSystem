package org.poo.bank.commands.account_commands.card_commands;

import org.poo.bank.account.Account;
import org.poo.bank.cards.Card;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class CheckCardStatus {

    /**
     * Executa comanda de verificare a statusului unui card pe baza detaliilor furnizate.
     * Daca cardul este asociat unui cont cu un sold mai mic decat minBalance-ul,
     * acesta va fii inghetat.
     *
     * @param command Comanda care contine detaliile cardului (cardNumber, timestamp).
     * @param users   Lista de utilizatori in care se cauta cardul.
     * @return Raspunsul in format Map, continand descrierea si timestamp-ul comenzii.
     */
    public Map<String, Object> execute(final CommandInput command, final List<User> users) {
        String cardNumber = command.getCardNumber();
        int timestamp = command.getTimestamp();

        User user = User.findByCardNumber(users, cardNumber);
        String description;

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            description = "Card not found";
            Map<String, Object> output = new HashMap<>();
            output.put("description", description);
            output.put("timestamp", timestamp);

            response.put("command", "checkCardStatus");
            response.put("output", output);
            response.put("timestamp", timestamp);
        } else {
            Account account = Account.findByCardNumber(user, cardNumber);
            if (account != null) {
                Card card = account.getCardByNumber(cardNumber);
                if (card != null && account.getBalance() <= account.getMinimumBalance()) {
                    description = "You have reached the minimum amount of funds, "
                            + "the card will be frozen";
                    Transaction transaction = new Transaction(
                            timestamp,
                            description,
                            account.getIban(),
                            null,
                            0,
                            null,
                            null,
                            cardNumber,
                            user.getEmail(),
                            null,
                            null,
                            null,
                            null,
                            "checkCardStatusFrozen"
                    );
                    user.addTransaction(transaction);
                }
            }
        }
        return response;
    }
}
