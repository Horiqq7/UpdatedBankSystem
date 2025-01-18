package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class SplitPayment {
    private final List<User> users;
    private static final List<String> SPLIT_PAYMENT_ACCOUNTS = new ArrayList<>();
    private static List<Double> amountForUsers;
    private static final Map<String, Boolean> ACCOUNTS_ACCEPTING_PAYMENT = new HashMap<>();
    private static String splitPaymentCurrency;
    private static int splitPaymentTimestamp;

    public SplitPayment(final List<User> users) {
        this.users = users;
    }

    /**
     * Proceseaza o plata impartita la mai multe conturi.
     *
     * @param command Obiectul de intrare care contine detaliile comenzii.
     * @return O lista cu rezultate.
     */
    public List<Map<String, Object>> splitPayment(final CommandInput command) {
        resetSplitPaymentState();

        List<Map<String, Object>> output = new ArrayList<>();
        List<String> accountIBANs = command.getAccounts();
        double totalAmount = command.getAmount();
        splitPaymentCurrency = command.getCurrency();
        splitPaymentTimestamp = command.getTimestamp();
        amountForUsers = command.getAmountForUsers();


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

            SPLIT_PAYMENT_ACCOUNTS.add(accountIBAN);
            ACCOUNTS_ACCEPTING_PAYMENT.put(accountIBAN, false);
        }

        Map<String, Object> success = new HashMap<>();
        success.put("description", "Split payment of " + String.format("%.2f", totalAmount)
                + " " + splitPaymentCurrency);
        success.put("accounts", accountIBANs);
        success.put("timestamp", splitPaymentTimestamp);
        output.add(success);

        return output;
    }


    public static List<String> getSplitPaymentAccounts() {
        return SPLIT_PAYMENT_ACCOUNTS;
    }

    public static List<Double> getAmountForUsers() {
        return amountForUsers;
    }

    public static Map<String, Boolean> getAccountsAcceptingPayment() {
        return ACCOUNTS_ACCEPTING_PAYMENT;
    }

    public static String getSplitPaymentCurrency() {
        return splitPaymentCurrency;
    }

    public static int getSplitPaymentTimestamp() {
        return splitPaymentTimestamp;
    }

    /**
     * Reseteaza starea asociata acestui tip de plata.
     */
    public static void resetSplitPaymentState() {
        SPLIT_PAYMENT_ACCOUNTS.clear();
        amountForUsers = null;
        ACCOUNTS_ACCEPTING_PAYMENT.clear();
        splitPaymentCurrency = null;
        splitPaymentTimestamp = 0;
    }
}
