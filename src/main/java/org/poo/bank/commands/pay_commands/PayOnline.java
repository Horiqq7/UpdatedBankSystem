package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.cards.Card;
import org.poo.bank.cards.OneTimeCard;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.user.User;
import org.poo.utils.Utils;
import org.poo.bank.cashback.Cashback;
import org.poo.fileio.CommerciantInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public final class PayOnline {

    private static final double SILVER_THRESHOLD_RON = 500;
    private static final double SILVER_COMMISSION_PERCENTAGE = 0.1;
    private static final double STANDARD_COMMISSION_PERCENTAGE = 0.2;
    private static final double PERCENTAGE = 100;

    private List<User> users;
    private List<CommerciantInput> commerciants;

    public PayOnline(final List<User> users, final List<CommerciantInput> commerciants) {
        this.users = users;
        this.commerciants = commerciants;
    }

    /**
     * Proceseaza o plata online folosind comanda furnizata.
     *
     * Metoda verifica daca utilizatorul exista, gaseste cardul specificat,
     * verifica fondurile suficiente, efectueaza conversia valutara daca este
     * necesar si actualizeaza statusul cardului si al contului dupa caz.
     *
     * Se gestioneaza mai multe scenarii, inclusiv fonduri insuficiente,
     * carduri frozen si distrugerea one time cardurilor.
     *
     * @param command Comanda care contine detaliile platii.
     *
     * @return O lista de harti de raspuns care contin statusul comenzii,
     * incluzand erorile, daca este cazul.
     *         O lista goala indica procesarea cu succes.
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

        if (!currency.equalsIgnoreCase(account.getCurrency())) {
            try {
                amount = ExchangeRateManager.getInstance()
                        .convertCurrency(currency, account.getCurrency(), amount);
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

        double comisionInAccountCurrency = 0.0;

        try {
            String userPlan = user.getPlan();
            if (userPlan == null) {
                userPlan = "standard";
            }

            switch (userPlan.toLowerCase()) {
                case "student":
                    break;

                case "silver":
                    double amountInRON = amount;
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

                    if (amountInRON > SILVER_THRESHOLD_RON) {
                        comisionInAccountCurrency = SILVER_COMMISSION_PERCENTAGE
                                / PERCENTAGE * amount;
                    }
                    break;

                case "gold":
                    break;

                case "standard":
                    comisionInAccountCurrency = STANDARD_COMMISSION_PERCENTAGE
                            / PERCENTAGE * amount;
                    break;

                default:
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

        if (availableBalance < amount + comisionInAccountCurrency
                && card.getStatus().equals("active")) {
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
                    null,
                    null,
                    "payOnlineInsufficientFunds"
            );
            user.addTransaction(transaction1);
            return output;
        }

        if (comisionInAccountCurrency > 0.0) {
            account.withdrawFunds(comisionInAccountCurrency);
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
                    null,
                    null,
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
                command.getCommerciant(),
                command.getAmount(),
                userPlan,
                commerciantObj,
                command.getCurrency(),
                account.getCurrency(),
                ExchangeRateManager.getInstance(),
                account);

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
                    true,
                    null,
                    null,
                    "newOneTimeCard"
            );
            user.addTransaction(newCardTransaction);
            account.addTransaction(newCardTransaction);
        }

        return Collections.emptyList();
    }

    private CommerciantInput findCommerciant(final String commerciantName) {
        for (CommerciantInput commerciant : commerciants) {
            if (commerciant.getCommerciant().equalsIgnoreCase(commerciantName)) {
                return commerciant;
            }
        }
        return null;
    }
}
