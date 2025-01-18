package org.poo.bank.commands.pay_commands;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AcceptSplitPayment {
    private final List<User> users;
    private final List<String> insufficientFundsAccounts = new ArrayList<>();
    private final List<String> allInsufficientFundsAccounts = new ArrayList<>();

    public AcceptSplitPayment(final List<User> users) {
        this.users = users;
    }

    /**
     * Proceseaza plata in mai multe transe pentru utilizatorii implicati.
     * Verifica daca utilizatorul care accepta plata exista si daca toate conturile implicate
     * au fonduri suficiente. Daca toate conturile sunt acceptate, se efectueaza tranzactia.
     *
     * @param command Comanda ce contine informatii despre utilizator si plata
     * @return un Map cu descrierea statusului procesului de plata,
     * incluzand eventualele conturi cu fonduri insuficiente
     */

    public Map<String, Object> acceptSplitPayment(final CommandInput command) {
        String email = command.getEmail();

        User acceptingUser = null;
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                acceptingUser = user;
                break;
            }
        }

        if (acceptingUser == null) {
            return Map.of("description", "User not found");
        }

        List<String> accountIBANs = SplitPayment.getSplitPaymentAccounts();
        if (accountIBANs == null || accountIBANs.isEmpty()) {
            return Map.of("description", "No split payment accounts found");
        }

        List<Double> amountForUsers = SplitPayment.getAmountForUsers();
        if (amountForUsers == null || amountForUsers.size() != accountIBANs.size()) {
            return Map.of("description", "Mismatch between accounts and amounts");
        }

        Map<String, Boolean> accountsAcceptingPayment = SplitPayment.getAccountsAcceptingPayment();

        for (int i = 0; i < accountIBANs.size(); i++) {
            String accountIBAN = accountIBANs.get(i);
            double amountForUser = amountForUsers.get(i);

            Account account = acceptingUser.getAccountByIBAN(accountIBAN);

            if (account != null) {
                if (account.getBalance() < amountForUser) {
                    insufficientFundsAccounts.add(accountIBAN);
                    if (!allInsufficientFundsAccounts.contains(accountIBAN)) {
                        allInsufficientFundsAccounts.add(accountIBAN);
                    }
                } else {
                    accountsAcceptingPayment.put(accountIBAN, true);
                }
            }
        }

        boolean allAccepted = accountsAcceptingPayment.values().stream().
                allMatch(Boolean::booleanValue);
        if (!allAccepted) {
            return Map.of("description", "Not all accounts have accepted the split payment");
        }

        if (!insufficientFundsAccounts.isEmpty()) {
            String lastProblematicAccount = insufficientFundsAccounts.
                    get(insufficientFundsAccounts.size() - 1);
            return Map.of(
                    "description", "One or more accounts have insufficient funds",
                    "lastProblematicAccount", lastProblematicAccount,
                    "timestamp", SplitPayment.getSplitPaymentTimestamp()
            );
        }

        String currency = SplitPayment.getSplitPaymentCurrency();
        int splitTimestamp = SplitPayment.getSplitPaymentTimestamp();

        for (int i = 0; i < accountIBANs.size(); i++) {
            String accountIBAN = accountIBANs.get(i);
            double amountForUser = amountForUsers.get(i);


            for (User user : users) {
                Account targetAccount = user.getAccountByIBAN(accountIBAN);
                if (targetAccount != null) {
                    targetAccount.withdrawFunds(amountForUser);
                    double totalAmount = amountForUsers.stream().
                            mapToDouble(Double::doubleValue).sum();

                    BigDecimal roundedAmount = new BigDecimal(totalAmount).
                            setScale(2, RoundingMode.HALF_UP);
                    double finalAmount = roundedAmount.doubleValue();
                    String finalAmountFormatted = String.format("%.2f", finalAmount);

                    Transaction deductionTransaction = new Transaction(
                            splitTimestamp,
                            "Split payment of " + finalAmountFormatted + " " + currency,
                            null,
                            accountIBAN,
                            finalAmount,
                            currency,
                            null,
                            null,
                            null,
                            null,
                            accountIBANs,
                            null,
                            null,
                            true,
                            amountForUsers,
                            "custom",
                            "splitPayment"
                    );

                    targetAccount.addTransaction(deductionTransaction);
                    user.addTransaction(deductionTransaction);
                }
            }
        }

        return Map.of(
                "description", "Split payment completed successfully",
                "allInsufficientFundsAccounts", allInsufficientFundsAccounts
        );
    }
}
