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

    public PayOnline(final List<User> users, final List<CommerciantInput> commerciants) {
        this.users = users;
        this.commerciants = commerciants;
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

        // Calculul comisionului in functie de planul utilizatorului
        double comision = 0.0;
        double comisionInAccountCurrency = 0.0;

        try {
            String userPlan = user.getPlan(); // Obținem planul utilizatorului
            if (userPlan == null) {
                userPlan = "standard"; // Dacă planul este null, presupunem planul standard
            }

            // Calculăm comisionul în funcție de planul utilizatorului
            switch (userPlan.toLowerCase()) {
                case "student":
                    // Nu se percepe comision pentru niciun fel de tranzacție
                    comision = 0.0;
                    break;

                case "silver":
                    // Dacă suma în RON depășește 500, aplicăm un comision de 0.1%
                    if (amount > 500) {
                        comision = 0.1 / 100 * amount;
                    }
                    break;

                case "gold":
                    // Nu se percepe comision pentru niciun fel de tranzacție
                    comision = 0.0;
                    break;

                case "standard":
                default:
                    // Planul standard percepe un comision de 0.2% din suma în RON
                    comision = 0.2 / 100 * amount;
                    break;
            }

            // Convertim comisionul în moneda contului expeditorului, dacă este necesar
            if (!account.getCurrency().equalsIgnoreCase("RON")) {
                comisionInAccountCurrency = ExchangeRateManager.getInstance().convertCurrency(
                        "RON",
                        account.getCurrency(),
                        comision
                );
            } else {
                comisionInAccountCurrency = comision; // Dacă moneda este deja RON, comisionul rămâne neschimbat
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

        // Verificăm dacă utilizatorul are suficienți bani pentru tranzacție plus comision
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
                    "payOnlineInsufficientFunds"
            );
            user.addTransaction(transaction1);
            return output;
        }

        // Aplicăm comisionul și retragem fondurile
        if (comisionInAccountCurrency > 0.0) {
            account.withdrawFunds(comisionInAccountCurrency);
        }

        // Retragem suma pentru tranzacție
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

        Cashback cashback = new Cashback();
        String userPlan = user.getPlan();
        double cashbackAmount = cashback.applyCashback(
                command.getCommerciant(),
                command.getAmount(),
                userPlan,
                findCommerciant(command.getCommerciant()),
                command.getCurrency(),
                account.getCurrency(),
                ExchangeRateManager.getInstance()
        );

        // Aplicarea cashback-ului
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

