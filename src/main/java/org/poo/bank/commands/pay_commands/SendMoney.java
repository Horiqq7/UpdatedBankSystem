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
    private final List<User> users;

    public SendMoney(final List<User> users) {
        this.users = users;
    }

    public List<Map<String, Object>> sendMoney(final CommandInput command) {
        List<Map<String, Object>> output = new ArrayList<>();
        String senderIBAN = command.getAccount();
        String receiverIBAN = command.getReceiver();
        double amount = command.getAmount();
        String description = command.getDescription();
        int timestamp = command.getTimestamp();

        // Verificăm dacă suma este validă
        if (amount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid amount");
            output.add(error);
            return output;
        }

        // Găsim contul expeditorului
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

        // Găsim contul destinatarului
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

        // Verificăm dacă expeditorul are suficiente fonduri

        // Calculăm suma convertită dacă monedele sunt diferite
        double convertedAmount = amount;
        double amountRON = amount;

        // Dacă monedele sunt diferite, efectuăm conversia
        if (!senderAccount.getCurrency().equalsIgnoreCase(receiverAccount.getCurrency())) {
            try {
                // Convertim suma în moneda destinatarului, folosind direct monedele implicate
                convertedAmount = ExchangeRateManager.getInstance().convertCurrency(
                        senderAccount.getCurrency(),
                        receiverAccount.getCurrency(),
                        amount
                );

                // Convertim suma în RON pentru calcularea comisionului, dacă expeditorul nu folosește RON
                if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                    amountRON = ExchangeRateManager.getInstance().convertCurrency(
                            senderAccount.getCurrency(),
                            "RON",
                            amount
                    );
                } else {
                    // Dacă expeditorul folosește deja RON, suma în RON este aceeași cu suma inițială
                    amountRON = amount;
                }
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("description", "Exchange rates not available");
                output.add(error);
                return output;
            }
        }




        double comision = 0.0;
        double comisionInAccountCurrency = 0.0; // Inițializăm variabila pentru comisionul în moneda contului

        try {
            // Obținem planul utilizatorului
            String userPlan = senderUser.getPlan();

            // Convertim suma în RON pentru calculul comisionului, dacă moneda nu este deja RON
            if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                amountRON = ExchangeRateManager.getInstance().convertCurrency(
                        senderAccount.getCurrency(),
                        "RON",
                        amount
                );
            } else {
                amountRON = amount; // Dacă moneda este deja RON, nu facem conversia
            }

            // Calculăm comisionul în funcție de planul utilizatorului
            switch (userPlan.toLowerCase()) {
                case "student":
                    // Nu se percepe comision pentru niciun fel de tranzacție
                    comision = 0.0;
                    break;

                case "silver":
                    // Dacă suma în RON depășește 500, aplicăm un comision de 0.1%
                    if (amountRON > 500) {
                        comision = 0.1 / 100 * amountRON;
                    }
                    break;

                case "gold":
                    // Nu se percepe comision pentru niciun fel de tranzacție
                    comision = 0.0;
                    break;

                case "standard":
                default:
                    // Planul standard percepe un comision de 0.2% din suma în RON
                    comision = 0.2 / 100 * amountRON;
                    break;
            }

            // Convertim comisionul din RON în moneda contului expeditorului, dacă este necesar
            if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                comisionInAccountCurrency = ExchangeRateManager.getInstance().convertCurrency(
                        "RON",
                        senderAccount.getCurrency(),
                        comision
                );
            } else {
                comisionInAccountCurrency = comision; // Dacă moneda este deja RON, comisionul rămâne neschimbat
            }

            // Retragem comisionul din contul expeditorului doar dacă există un comision de aplicat
            if (comisionInAccountCurrency > 0.0) {
                // Verificăm dacă expeditorul are suficientă balanță pentru comision
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

//        if (senderAccount.getBalance() < amount) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("description", "Insufficient funds in sender account after commission");
//            output.add(error);
//            return output;
//        }

        System.out.println(amount + " " + command.getTimestamp());
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
                "sendMoney"
        );

        senderUser.addTransaction(senderTransaction);
        senderAccount.addTransaction(senderTransaction);

        receiverUser.addTransaction(receiverTransaction);
        receiverAccount.addTransaction(receiverTransaction);

        return Collections.emptyList();
    }
}