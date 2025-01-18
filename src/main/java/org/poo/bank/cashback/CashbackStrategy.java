package org.poo.bank.cashback;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;

public interface CashbackStrategy {
    double calculateCashback(
            String email, String commerciant, String category, double transactionAmount,
            String userPlan, String transactionCurrency, String accountCurrency,
            ExchangeRateManager exchangeRateManager, Account account
    );
}

