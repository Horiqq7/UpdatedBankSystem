package org.poo.bank.cashback;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;

public final class NrOfTransactionsCashbackStrategy implements CashbackStrategy {
    private static final double FOOD_CASHBACK_PERCENTAGE = 0.02;
    private static final double CLOTHES_CASHBACK_PERCENTAGE = 0.05;
    private static final double TECH_CASHBACK_PERCENTAGE = 0.10;

    private static final int FOOD_TRANSACTION_THRESHOLD = 2;
    private static final int CLOTHES_TRANSACTION_THRESHOLD = 5;
    private static final int TECH_TRANSACTION_THRESHOLD = 10;

    public double calculateCashback(
            final String email, final String commerciant, final String category,
            final double transactionAmount, final String userPlan,
            final String transactionCurrency, final String accountCurrency,
            final ExchangeRateManager exchangeRateManager, final Account account
    ) {
        int transactionCount = account.getTransactionsCountForCommerciant(commerciant,
                true);
        double cashbackPercentage = 0.0;

        if ("Food".equals(category) && transactionCount
                == FOOD_TRANSACTION_THRESHOLD) {
            cashbackPercentage = FOOD_CASHBACK_PERCENTAGE;
        } else if ("Clothes".equals(category) && transactionCount
                == CLOTHES_TRANSACTION_THRESHOLD) {
            cashbackPercentage = CLOTHES_CASHBACK_PERCENTAGE;
        } else if ("Tech".equals(category) && transactionCount
                == TECH_TRANSACTION_THRESHOLD) {
            cashbackPercentage = TECH_CASHBACK_PERCENTAGE;
        }

        if (cashbackPercentage > 0) {
            double cashbackAmount = cashbackPercentage * transactionAmount;
            if (!transactionCurrency.equalsIgnoreCase(accountCurrency)) {
                cashbackAmount *= exchangeRateManager.getExchangeRate(transactionCurrency,
                        accountCurrency);
            }
            return cashbackAmount;
        }

        return 0.0;
    }
}
