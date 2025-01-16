package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.*;

public final class SplitPayment {
    private final List<User> users;
    private static final List<String> splitPaymentAccounts = new ArrayList<>();
    private static List<Double> amountForUsers;
    private static final Map<String, Boolean> accountsAcceptingPayment = new HashMap<>();
    private static String splitPaymentCurrency; // Moneda pentru split payment
    private static int splitPaymentTimestamp; // Timestamp pentru split payment

    public SplitPayment(final List<User> users) {
        this.users = users;
    }

    public List<Map<String, Object>> splitPayment(final CommandInput command) {
        // Resetăm starea split payment pentru a preveni conflictele
        resetSplitPaymentState();

        List<Map<String, Object>> output = new ArrayList<>();
        List<String> accountIBANs = command.getAccounts();
        double totalAmount = command.getAmount();
        splitPaymentCurrency = command.getCurrency(); // Salvăm moneda
        splitPaymentTimestamp = command.getTimestamp(); // Salvăm timestamp-ul
        amountForUsers = command.getAmountForUsers(); // Salvăm sumele pentru utilizatori

        System.out.println("Processing splitPayment command...");
        System.out.println("Initial state of splitPaymentAccounts: " + splitPaymentAccounts);
        System.out.println("Initial state of amountForUsers: " + amountForUsers);
        System.out.println("Initial state of accountsAcceptingPayment: " + accountsAcceptingPayment);

        if (totalAmount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid amount");
            output.add(error);
            return output;
        }

        if (amountForUsers == null || amountForUsers.size() != accountIBANs.size()) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Mismatch between accounts and amounts");
            output.add(error);
            return output;
        }

        for (int i = 0; i < accountIBANs.size(); i++) {
            String accountIBAN = accountIBANs.get(i);
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
                error.put("timestamp", splitPaymentTimestamp);
                output.add(error);
                return output;
            }

            splitPaymentAccounts.add(accountIBAN);
            accountsAcceptingPayment.put(accountIBAN, false); // Inițial, contul nu a acceptat plata
        }

        Map<String, Object> success = new HashMap<>();
        success.put("description", "Split payment of " + String.format("%.2f", totalAmount) + " " + splitPaymentCurrency);
        success.put("accounts", accountIBANs);
        success.put("timestamp", splitPaymentTimestamp);
        output.add(success);

        return output;
    }


    public static List<String> getSplitPaymentAccounts() {
        return splitPaymentAccounts;
    }

    public static List<Double> getAmountForUsers() {
        return amountForUsers;
    }

    public static Map<String, Boolean> getAccountsAcceptingPayment() {
        return accountsAcceptingPayment;
    }

    public static String getSplitPaymentCurrency() {
        return splitPaymentCurrency;
    }

    public static int getSplitPaymentTimestamp() {
        return splitPaymentTimestamp;
    }

    public static void resetSplitPaymentState() {
        splitPaymentAccounts.clear();
        amountForUsers = null;
        accountsAcceptingPayment.clear();
        splitPaymentCurrency = null;
        splitPaymentTimestamp = 0;
    }
}