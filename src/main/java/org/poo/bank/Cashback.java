package org.poo.bank;

import org.poo.bank.account.Account;
import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.CommerciantInput;

import java.util.HashMap;
import java.util.Map;

public class Cashback {
    private Map<String, Map<String, Integer>> transactionsCount;  // Harta pentru tranzacții per utilizator și comerciant
    private Map<String, Map<String, Double>> spendingTotal; // Totalul cheltuielilor pentru fiecare utilizator și comerciant

    public Cashback() {
        transactionsCount = new HashMap<>();
        spendingTotal = new HashMap<>();
    }

    public double applyCashback(String email, String accountId, String commerciant, double transactionAmount,
                                String userPlan, CommerciantInput commerciantInput, String transactionCurrency,
                                String accountCurrency, ExchangeRateManager exchangeRateManager, Account account) {
        if (userPlan == null) {
            userPlan = "standard"; // Comportament implicit
        }

        String cashbackStrategy = commerciantInput.getCashbackStrategy();
        double cashback = 0.0;

        if ("nrOfTransactions".equalsIgnoreCase(cashbackStrategy)) {
            cashback = handleNrOfTransactionsCashback(
                    account,
                    commerciant,
                    transactionAmount,
                    commerciantInput.getType(),
                    transactionCurrency, // Folosim direct transactionCurrency
                    accountCurrency,      // Adăugăm accountCurrency
                    exchangeRateManager   // Adăugăm exchangeRateManager
            );
        }
        else if ("spendingThreshold".equalsIgnoreCase(cashbackStrategy)) {
            cashback = handleSpendingThresholdCashback(email, accountId, commerciant, transactionAmount, userPlan,
                    transactionCurrency, accountCurrency, exchangeRateManager);
        }

        return cashback;
    }

    private double handleNrOfTransactionsCashback(Account account, String commerciant, double transactionAmount, String category, String transactionCurrency, String accountCurrency, ExchangeRateManager exchangeRateManager) {
        // Obținem numărul de tranzacții valide din Account
        int transactionCount = account.getTransactionsCountForCommerciant(commerciant, true);  // True indică să numărăm doar tranzacțiile efectuate cu succes

        double cashbackPercentage = 0.0;
        System.out.println("nrOfTran " + transactionCount + " " + commerciant);

        // Stabilim procentajul de cashback pe baza numărului de tranzacții
        if (category.equals("Food") && transactionCount == 2) {
            cashbackPercentage = 0.02; // 2% cashback pentru 2 tranzacții la Food
        } else if (category.equals("Clothes") && transactionCount == 5) {
            cashbackPercentage = 0.05; // 5% cashback pentru 5 tranzacții la Clothes
        } else if (category.equals("Tech") && transactionCount == 10) {
            cashbackPercentage = 0.10; // 10% cashback pentru 10 tranzacții la Tech
        }

        if (cashbackPercentage > 0) {
            double cashbackAmount = cashbackPercentage * transactionAmount;

            // Dacă moneda tranzacției și moneda contului sunt diferite, facem conversia
            if (!transactionCurrency.equalsIgnoreCase(accountCurrency)) {
                cashbackAmount = cashbackAmount * exchangeRateManager.getExchangeRate(transactionCurrency, accountCurrency);
            }

            return cashbackAmount;
        }

        return 0.0; // Dacă nu se aplică cashback, returnăm 0
    }



    private double handleSpendingThresholdCashback(
            String email, String accountId, String commerciant,
            double transactionAmount, String userPlan,
            String transactionCurrency, String accountCurrency,
            ExchangeRateManager exchangeRateManager) {

        double transactionAmountInRON = transactionCurrency.equalsIgnoreCase("RON")
                ? transactionAmount
                : exchangeRateManager.convertCurrency(transactionCurrency, "RON", transactionAmount);

        // Actualizăm totalul cheltuielilor pentru utilizator și comerciant
        Map<String, Double> accountSpendingTotal = spendingTotal.getOrDefault(email, new HashMap<>());
        accountSpendingTotal.put(commerciant, accountSpendingTotal.getOrDefault(commerciant, 0.0) + transactionAmountInRON);
        spendingTotal.put(email, accountSpendingTotal);

        double totalSpent = accountSpendingTotal.get(commerciant);
        double cashbackPercentage = 0.0;

        if (totalSpent >= 500) {
            cashbackPercentage = getPlanPercentage(userPlan, 0.25, 0.5, 0.7);
        } else if (totalSpent >= 300) {
            cashbackPercentage = getPlanPercentage(userPlan, 0.2, 0.4, 0.55);
        } else if (totalSpent >= 100) {
            cashbackPercentage = getPlanPercentage(userPlan, 0.1, 0.3, 0.5);
        }

        double cashbackInRON = cashbackPercentage * transactionAmountInRON;
        double cashbackInAccountCurrency = exchangeRateManager.convertCurrency("RON", accountCurrency, cashbackInRON);
        System.out.println("debug cashback threshold " + cashbackInAccountCurrency + " " + commerciant);

        return cashbackInAccountCurrency;
    }

    private double getPlanPercentage(String userPlan, double standard, double silver, double gold) {
        switch (userPlan.toLowerCase()) {
            case "gold":
                return gold / 100;
            case "silver":
                return silver / 100;
            default:
                return standard / 100;
        }
    }
}
