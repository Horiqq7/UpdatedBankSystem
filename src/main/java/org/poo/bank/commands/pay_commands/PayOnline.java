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
    private List<CommerciantInput> commerciants;
    private Map<String, Map<String, Integer>> transactionsPerCommerciant = new HashMap<>();

    public PayOnline(final List<User> users, final List<CommerciantInput> commerciants) {
        this.users = users;
        this.commerciants = commerciants;
    }

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


        double comisionInAccountCurrency = 0.0; // Comision în moneda contului

        try {
            String userPlan = user.getPlan(); // Obținem planul utilizatorului
            if (userPlan == null) {
                userPlan = "standard"; // Dacă planul este null, presupunem planul standard
            }

            // Calculăm comisionul în funcție de planul utilizatorului
            switch (userPlan.toLowerCase()) {
                case "student":
                    break;

                case "silver":
                    // Convertim suma în RON dacă este necesar
                    double amountInRON = amount;
                    System.out.println(amount);
                    if (!currency.equalsIgnoreCase("RON")) {
                        try {
                            amountInRON = ExchangeRateManager.getInstance()
                                    .convertCurrency(currency, "RON", amount);
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
                    System.out.println(amountInRON);

                    if (amountInRON > 500) {
                        comisionInAccountCurrency = 0.1 / 100 * amount;
                    }
                    break;

                case "gold":
                    break;

                case "standard":
                    comisionInAccountCurrency = 0.2 / 100 * amount;
                    System.out.println("comisionul in account currency " + comisionInAccountCurrency);
                    System.out.println(amount);
                    break;
            }

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorNode = new HashMap<>();
            errorNode.put("description", "Error calculating commission");
            Map<String, Object> response = new HashMap<>();
            response.put("command", "payOnline");
            response.put("output", errorNode);
            output.add(response);
            return output;
        }

        if (availableBalance < amount + comisionInAccountCurrency && card.getStatus().equals("active")) {
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
                    true,
                    "payOnlineInsufficientFunds"
            );
            user.addTransaction(transaction1);
            return output;
        }

        if (comisionInAccountCurrency > 0.0) {
            account.withdrawFunds(comisionInAccountCurrency); // Retragem comisionul din contul expeditorului
        }



        account.withdrawFunds(amount);
        if (amount > 0) {
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
                    true,
                    "payOnline"
            );

            user.addTransaction(transaction);
            account.addTransaction(transaction);
        }


        Cashback cashback = new Cashback();
        String userPlan = user.getPlan();
        CommerciantInput commerciantObj = findCommerciant(command.getCommerciant());
        double cashbackAmount = cashback.applyCashback(
                user.getEmail(),
                account.getIban(),
                command.getCommerciant(),
                command.getAmount(),
                userPlan,
                commerciantObj,
                command.getCurrency(),
                account.getCurrency(),
                ExchangeRateManager.getInstance(),
                account  // Adaugă obiectul `account` aici
        );

        account.addFunds(cashbackAmount);

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
                    true,
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
                    true,
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
