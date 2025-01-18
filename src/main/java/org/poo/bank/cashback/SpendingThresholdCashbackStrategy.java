package org.poo.bank.cashback;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;

import java.util.HashMap;
import java.util.Map;

public final class SpendingThresholdCashbackStrategy implements CashbackStrategy {
    private static final double SPENDING_THRESHOLD_100 = 100.0;
    private static final double SPENDING_THRESHOLD_300 = 300.0;
    private static final double SPENDING_THRESHOLD_500 = 500.0;

    private static final double STANDARD_STUDENT_100 = 0.10;
    private static final double STANDARD_STUDENT_300 = 0.20;
    private static final double STANDARD_STUDENT_500 = 0.25;

    private static final double SILVER_100 = 0.30;
    private static final double SILVER_300 = 0.40;
    private static final double SILVER_500 = 0.50;

    private static final double GOLD_100 = 0.50;
    private static final double GOLD_300 = 0.55;
    private static final double GOLD_500 = 0.70;

    private static final double PERCENTAGE = 100;

    private final Map<String, Map<String, Double>> spendingTotal = new HashMap<>();

    public double calculateCashback(
            final String email, final String commerciant, final String category,
            final double transactionAmount, final String userPlan,
            final String transactionCurrency, final String accountCurrency,
            final ExchangeRateManager exchangeRateManager, final Account account
    ) {
        double transactionAmountInRON = transactionCurrency.equalsIgnoreCase("RON")
                ? transactionAmount
                : exchangeRateManager.convertCurrency(transactionCurrency,
                "RON", transactionAmount);

        Map<String, Double> accountSpendingTotal
                = spendingTotal.getOrDefault(email, new HashMap<>());
        accountSpendingTotal.put(commerciant,
                accountSpendingTotal.getOrDefault(commerciant, 0.0)
                        + transactionAmountInRON);
        spendingTotal.put(email, accountSpendingTotal);

        double totalSpent = accountSpendingTotal.get(commerciant);
        double cashbackPercentage = 0.0;

        if (totalSpent >= SPENDING_THRESHOLD_500) {
            cashbackPercentage = getPlanPercentage(userPlan,
                    STANDARD_STUDENT_500, SILVER_500, GOLD_500);
        } else if (totalSpent >= SPENDING_THRESHOLD_300) {
            cashbackPercentage = getPlanPercentage(userPlan,
                    STANDARD_STUDENT_300, SILVER_300, GOLD_300);
        } else if (totalSpent >= SPENDING_THRESHOLD_100) {
            cashbackPercentage = getPlanPercentage(userPlan,
                    STANDARD_STUDENT_100, SILVER_100, GOLD_100);
        }

        double cashbackInRON = cashbackPercentage * transactionAmountInRON;
        return exchangeRateManager.convertCurrency("RON",
                accountCurrency, cashbackInRON);
    }

    private double getPlanPercentage(final String userPlan, final double standard,
                                     final double silver, final double gold) {
        return switch (userPlan) {
            case "gold" -> gold / PERCENTAGE;
            case "silver" -> silver / PERCENTAGE;
            default -> standard / PERCENTAGE;
        };
    }
}
