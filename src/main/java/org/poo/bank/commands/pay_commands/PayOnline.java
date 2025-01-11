package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.cards.Card;
import org.poo.bank.cards.OneTimeCard;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.user.User;
import org.poo.utils.Utils;
import org.poo.bank.Cashback; // Adăugăm clasa Cashback
import org.poo.fileio.CommerciantInput; // Pentru comerciant

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public final class PayOnline {
    private List<User> users;
    private List<CommerciantInput> commerciants; // Adăugăm lista de comercianți

    public PayOnline(final List<User> users, final List<CommerciantInput> commerciants) {
        this.users = users;
        this.commerciants = commerciants; // Inițializăm lista de comercianți
    }

    /**
     * Procesează o plată online folosind comanda furnizată.
     */
    public List<Map<String, Object>> payOnline(final CommandInput command) {
        List<Map<String, Object>> output = new ArrayList<>();

        User user = User.findByEmail(users, command.getEmail());
        if (user == null) {
            Map<String, Object> errorNode = new HashMap<>();
            errorNode.put("description", "User not found");
            Map<String, Object> response = new HashMap<>();
            response.put("command", "payOnline");
            response.put("output", errorNode);
            output.add(response);
            return output;
        }

        Account account = null;
        Card card = null;

        for (Account acc : user.getAccounts()) {
            card = acc.getCardByNumber(command.getCardNumber());
            if (card != null) {
                account = acc;
                break;
            }
        }

        if (card == null) {
            Map<String, Object> errorNode = new HashMap<>();
            errorNode.put("description", "Card not found");

            Map<String, Object> response = new HashMap<>();
            response.put("command", "payOnline");
            response.put("output", errorNode);
            output.add(response);

            return output;
        }

        double amount = command.getAmount();
        String currency = command.getCurrency();
        double availableBalance = account.getBalance();

        // Conversie valutara daca este necesar
        if (!currency.equalsIgnoreCase(account.getCurrency())) {
            try {
                amount = ExchangeRateManager.getInstance()
                        .convertCurrency(currency, account.getCurrency(),
                                amount);
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorNode = new HashMap<>();
                errorNode.put("description", "Exchange rates not available");
                Map<String, Object> response = new HashMap<>();
                response.put("command", "payOnline");
                response.put("output", errorNode);
                output.add(response);

                return output;
            }
        }

        if (availableBalance < amount && card.getStatus().equals("active")) {
            Transaction transaction1 = new Transaction(
                    command.getTimestamp(),
                    "Insufficient funds",
                    null,
                    null,
                    0,
                    account.getCurrency(),
                    "payment",
                    command.getCardNumber(),
                    user.getEmail(), null,
                    null,
                    null,
                    null,
                    "payOnlineInsufficientFunds"
            );
            user.addTransaction(transaction1);
            return output;
        }

        if (amount > availableBalance - account.getMinimumBalance()) {
            card.setStatus("frozen");
        }

        if (card.getStatus().equals("frozen")) {
            Transaction transaction = new Transaction(
                    command.getTimestamp(),
                    "The card is frozen",
                    account.getIban(),
                    command.getCommerciant(),
                    0,
                    account.getCurrency(),
                    "payment",
                    command.getCardNumber(),
                    user.getEmail(),
                    command.getCommerciant(),
                    null,
                    null,
                    null,
                    "payOnlineCardIsFrozen"
            );

            user.addTransaction(transaction);
            return output;
        }

        CommerciantInput commerciantInput = findCommerciant(command.getCommerciant());
        if (commerciantInput == null) {
            Map<String, Object> errorNode = new HashMap<>();
            errorNode.put("description", "Commerciant not found");
            Map<String, Object> response = new HashMap<>();
            response.put("command", "payOnline");
            response.put("output", errorNode);
            output.add(response);
            return output;
        }

        Cashback cashback = new Cashback();
        String userPlan = user.getPlan();
        double cashbackAmount = cashback.applyCashback(
                command.getCommerciant(),
                command.getAmount(), // Suma tranzacției în moneda tranzacției
                userPlan,
                commerciantInput,
                command.getCurrency(), // Moneda tranzacției
                account.getCurrency(), // Moneda contului
                ExchangeRateManager.getInstance()
        );

        // Retragerea fondurilor pentru tranzacție
        account.withdrawFunds(amount);
        Transaction transaction = new Transaction(
                command.getTimestamp(),
                "Card payment",
                account.getIban(),
                command.getCommerciant(),
                amount,
                account.getCurrency(),
                "payment",
                command.getCardNumber(),
                user.getEmail(),
                command.getCommerciant(),
                null,
                null,
                null,
                "payOnline"
        );

        user.addTransaction(transaction);
        account.addTransaction(transaction);

        // Aplicarea cashback-ului (se adaugă la balanța contului)
        if (cashbackAmount > 0) {
            account.addFunds(cashbackAmount);
        }

        if (card instanceof OneTimeCard) {
            Transaction destroyCardTransaction = new Transaction(
                    command.getTimestamp(),
                    "The card has been destroyed",
                    account.getIban(),
                    null,
                    0,
                    account.getCurrency(),
                    "other",
                    card.getCardNumber(),
                    user.getEmail(),
                    null,
                    null,
                    null,
                    null,
                    "destroyOneTimeCard"
            );
            user.addTransaction(destroyCardTransaction);
            account.addTransaction(destroyCardTransaction);
            String newCardNumber = Utils.generateCardNumber();
            card.setCardNumber(newCardNumber);
            card.setStatus("active");

            Transaction newCardTransaction = new Transaction(
                    command.getTimestamp(),
                    "New card created",
                    account.getIban(),
                    null,
                    0,
                    account.getCurrency(),
                    "other",
                    newCardNumber,
                    user.getEmail(),
                    null,
                    null,
                    null,
                    null,
                    "newOneTimeCard"
            );
            user.addTransaction(newCardTransaction);
            account.addTransaction(newCardTransaction);
        }

        return Collections.emptyList();
    }

    private CommerciantInput findCommerciant(String commerciantName) {
        for (CommerciantInput commerciant : commerciants) {
            if (commerciant.getCommerciant().equalsIgnoreCase(commerciantName)) {
                return commerciant;
            }
        }
        return null;
    }
}
