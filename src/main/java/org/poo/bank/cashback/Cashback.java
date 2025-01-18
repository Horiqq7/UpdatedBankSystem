package org.poo.bank.cashback;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.fileio.CommerciantInput;

public final class Cashback {
    private CashbackStrategy strategy;

    public void setStrategy(CashbackStrategy strategy) {
        this.strategy = strategy;
    }

    public double applyCashback(
            final String email, final String commerciant,
            final double transactionAmount, String userPlan,
            final CommerciantInput commerciantInput,
            final String transactionCurrency, final String accountCurrency,
            final ExchangeRateManager exchangeRateManager, final Account account
    ) {
        if (userPlan == null) {
            userPlan = "standard";
        }

        String cashbackStrategy = commerciantInput.getCashbackStrategy();
        String category = commerciantInput.getType();

        switch (cashbackStrategy) {
            case "nrOfTransactions" -> setStrategy(new NrOfTransactionsCashbackStrategy());
            case "spendingThreshold" -> setStrategy(new SpendingThresholdCashbackStrategy());
            default -> throw new IllegalArgumentException("Unknown cashback strategy");
        }

        return strategy.calculateCashback(email, commerciant, category, transactionAmount, userPlan,
                transactionCurrency, accountCurrency, exchangeRateManager, account);
    }
}
