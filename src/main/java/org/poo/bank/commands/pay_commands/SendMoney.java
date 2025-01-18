package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public final class SendMoney {
    private static final double STUDENT_COMMISSION = 0.0;
    private static final double SILVER_THRESHOLD_RON = 500.0;
    private static final double SILVER_COMMISSION_PERCENTAGE = 0.1;
    private static final double STANDARD_COMMISSION_PERCENTAGE = 0.2;
    private static final double PERCENTAGE_DIVISOR = 100.0;

    private final List<User> users;

    public SendMoney(final List<User> users) {
        this.users = users;
    }

    /**
     * Transfer bani de la un utilizator la altul.
     * Aceasta metoda verifica daca contul expeditorului si destinatarului exista,
     * daca expeditorul are suficiente fonduri si se efectueaza tranzactia.
     * In cazul in care monedele diferite sunt implicate, se efectueaza conversia valutara.
     * Daca tranzactia nu poate fi realizata, sunt adaugate erori corespunzatoare in
     * lista de iesire.
     *
     * @param command comanda care contine detaliile tranzactiei.
     * @return lista de erori, daca exista, sau lista goala daca tranzactia a avut succes
     */
    public List<Map<String, Object>> sendMoney(final CommandInput command) {
        List<Map<String, Object>> output = new ArrayList<>();
        String senderIBAN = command.getAccount();
        String receiverIBAN = command.getReceiver();
        double amount = command.getAmount();
        String description = command.getDescription();
        int timestamp = command.getTimestamp();

        if (amount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid amount");
            output.add(error);
            return output;
        }

        Account senderAccount = null;
        User senderUser = null;
        for (User user : users) {
            senderAccount = user.getAccountByIBAN(senderIBAN);
            if (senderAccount != null) {
                senderUser = user;
                break;
            }
        }

        if (senderAccount == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Sender account not found");
            output.add(error);
            return output;
        }

        Account receiverAccount = null;
        User receiverUser = null;
        for (User user : users) {
            receiverAccount = user.getAccountByIBAN(receiverIBAN);
            if (receiverAccount != null) {
                receiverUser = user;
                break;
            }
        }

        if (receiverAccount == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "User not found");
            output.add(error);
            return output;
        }

        double convertedAmount = amount;
        double amountRON;

        if (!senderAccount.getCurrency().equalsIgnoreCase(receiverAccount.getCurrency())) {
            try {
                convertedAmount = ExchangeRateManager.getInstance().convertCurrency(
                        senderAccount.getCurrency(),
                        receiverAccount.getCurrency(),
                        amount
                );
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("description", "Exchange rates not available");
                output.add(error);
                return output;
            }
        }

        double comision = 0.0;
        double comisionInAccountCurrency = 0.0;

        try {
            String userPlan = senderUser.getPlan();
            if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                amountRON = ExchangeRateManager.getInstance().convertCurrency(
                        senderAccount.getCurrency(),
                        "RON",
                        amount
                );
            } else {
                amountRON = amount;
            }

            switch (userPlan) {
                case "student":
                    comision = STUDENT_COMMISSION;
                    break;

                case "silver":
                    if (amountRON > SILVER_THRESHOLD_RON) {
                        comision = (SILVER_COMMISSION_PERCENTAGE / PERCENTAGE_DIVISOR) * amountRON;
                    }
                    break;

                case "gold":
                    comision = 0;
                    break;

                case "standard":
                default:
                    comision = (STANDARD_COMMISSION_PERCENTAGE / PERCENTAGE_DIVISOR) * amountRON;
                    break;
            }

            if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                comisionInAccountCurrency = ExchangeRateManager.getInstance().convertCurrency(
                        "RON",
                        senderAccount.getCurrency(),
                        comision
                );
            } else {
                comisionInAccountCurrency = comision;
            }

            if (comisionInAccountCurrency > 0.0) {
                if (senderAccount.getBalance() < comisionInAccountCurrency) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("description", "Insufficient funds to cover the commission");
                    output.add(error);
                    return output;
                }

                if (senderAccount.getBalance() > amount + comisionInAccountCurrency) {
                    senderAccount.withdrawFunds(comisionInAccountCurrency);
                }
            }

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Exchange rates not available for commission calculation");
            output.add(error);
            return output;
        }

        if (senderAccount.getBalance() < amount + comisionInAccountCurrency) {
            Transaction insufficientFundsTransaction = new Transaction(
                    timestamp,
                    "Insufficient funds",
                    senderIBAN,
                    receiverIBAN,
                    amount,
                    senderAccount.getCurrency(),
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
                    "sendMoneyInsufficientFunds"
            );
            senderUser.addTransaction(insufficientFundsTransaction);
            senderAccount.addTransaction(insufficientFundsTransaction);

            Map<String, Object> error = new HashMap<>();
            error.put("description", "Insufficient funds in sender account");
            output.add(error);
            return output;
        }

        senderAccount.withdrawFunds(amount);
        receiverAccount.addFunds(convertedAmount);

        Transaction senderTransaction = new Transaction(
                timestamp,
                description,
                senderIBAN,
                receiverIBAN,
                Double.parseDouble(String.format("%.14f", amount)),
                senderAccount.getCurrency(),
                "sent",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "sendMoney"
        );

        Transaction receiverTransaction = new Transaction(
                timestamp,
                description,
                senderIBAN,
                receiverIBAN,
                Double.parseDouble(String.format("%.14f", convertedAmount)),
                receiverAccount.getCurrency(),
                "received",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "sendMoney"
        );

        senderUser.addTransaction(senderTransaction);
        senderAccount.addTransaction(senderTransaction);

        receiverUser.addTransaction(receiverTransaction);
        receiverAccount.addTransaction(receiverTransaction);

        return Collections.emptyList();
    }
}
