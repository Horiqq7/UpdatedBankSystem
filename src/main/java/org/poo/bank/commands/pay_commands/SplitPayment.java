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

public final class SplitPayment {
    private final List<User> users;

    public SplitPayment(final List<User> users) {
        this.users = users;
    }

    /**
     * Proceseaza o comanda de tip splitPayment distribuind suma specificata intre conturile date.
     *
     * @param command Comanda de plata care contine informatiile necesare pentru procesare.
     * @return O lista de erori sau tranzactii efectuate.
     */
    public List<Map<String, Object>> splitPayment(final CommandInput command) {
        List<Map<String, Object>> output = new ArrayList<>();
        List<String> accountIBANs = command.getAccounts();
        double amount = command.getAmount();
        String currency = command.getCurrency();
        int timestamp = command.getTimestamp();

        if (amount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid amount");
            output.add(error);
            return output;
        }

        double splitAmount = amount / accountIBANs.size();
        ExchangeRateManager exchangeRateManager = ExchangeRateManager.getInstance();

        Account problematicAccount = null;

        for (String accountIBAN : accountIBANs) {
            Account account = null;

            for (User u : users) {
                account = u.getAccountByIBAN(accountIBAN);
                if (account != null) {
                    break;
                }
            }

            if (account == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("description", "Account not found: " + accountIBAN);
                error.put("involvedAccounts", accountIBANs);
                error.put("timestamp", timestamp);
                output.add(error);
                return output;
            }

            double convertedAmount = splitAmount;
            if (!currency.equalsIgnoreCase(account.getCurrency())) {
                try {
                    convertedAmount = exchangeRateManager.convertCurrency(currency,
                            account.getCurrency(), splitAmount);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("description", "Conversion rate not available for "
                            + currency + " to " + account.getCurrency());
                    error.put("involvedAccounts", accountIBANs);
                    error.put("timestamp", timestamp);
                    output.add(error);
                    return output;
                }
            }

            if (account.getBalance() < convertedAmount) {
                problematicAccount = account;
            }
        }

        if (problematicAccount != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("amount", splitAmount);
            error.put("currency", currency);
            error.put("description", "Split payment of "
                    + String.format("%.2f", amount) + " " + currency);
            error.put("error", "Account " + problematicAccount.getIban()
                    + " has insufficient funds for a split payment.");
            error.put("involvedAccounts", accountIBANs);
            error.put("timestamp", timestamp);

            for (String accountIBAN : accountIBANs) {
                User user = null;
                Account account = null;

                for (User u : users) {
                    account = u.getAccountByIBAN(accountIBAN);
                    if (account != null) {
                        user = u;
                        break;
                    }
                }

                if (user != null && account != null) {
                    Transaction errorTransaction = new Transaction(
                            timestamp,
                            "Split payment of " + String.format("%.2f", amount) + " " + currency,
                            null,
                            accountIBAN,
                            splitAmount,
                            currency,
                            null,
                            null,
                            null,
                            null,
                            accountIBANs,
                            "Account " + problematicAccount.getIban()
                                    + " has insufficient funds for a split payment.",
                            null,
                            "splitPaymentError"
                    );
                    user.addTransaction(errorTransaction);
                    account.addTransaction(errorTransaction);
                }
            }

            output.add(error);
            return output;
        }

        for (String accountIBAN : accountIBANs) {
            Account account = null;
            User user = null;

            for (User u : users) {
                account = u.getAccountByIBAN(accountIBAN);
                if (account != null) {
                    user = u;
                    break;
                }
            }

            double convertedAmount = splitAmount;
            if (!currency.equalsIgnoreCase(account.getCurrency())) {
                convertedAmount = exchangeRateManager.convertCurrency(currency,
                        account.getCurrency(), splitAmount);
            }

            account.setBalance(account.getBalance() - convertedAmount);

            Transaction splitTransaction = new Transaction(
                    timestamp,
                    "Split payment of " + String.format("%.2f", amount) + " " + currency,
                    null,
                    accountIBAN,
                    splitAmount,
                    currency,
                    null,
                    null,
                    null,
                    null,
                    accountIBANs,
                    null,
                    null,
                    "splitPayment"
            );

            user.addTransaction(splitTransaction);
            account.addTransaction(splitTransaction);
        }

        return output;
    }
}
