package org.poo.bank;

import org.poo.bank.exchange_rates.ExchangeRateManager;
import org.poo.fileio.CommerciantInput;

import java.util.HashMap;
import java.util.Map;

public class Cashback {
    private Map<String, Integer> transactionsCount;  // Contor pentru tranzacțiile realizate
    private Map<String, Double> spendingTotal;       // Totalul cheltuit pe comercianți

    public Cashback() {
        transactionsCount = new HashMap<>();
        spendingTotal = new HashMap<>();
    }

    public double applyCashback(String commerciant, double transactionAmount, String userPlan, CommerciantInput commerciantInput, String transactionCurrency, String accountCurrency, ExchangeRateManager exchangeRateManager) {
        if (userPlan == null) {
            userPlan = "standard"; // Comportament implicit
        }

        String cashbackStrategy = commerciantInput.getCashbackStrategy();
//        System.out.println(cashbackStrategy + " " + commerciant);
        double cashback = 0.0;

        if ("nrOfTransactions".equalsIgnoreCase(cashbackStrategy)) {
            cashback = handleNrOfTransactionsCashback(commerciant, transactionAmount, commerciantInput.getType());
        } else if ("spendingThreshold".equalsIgnoreCase(cashbackStrategy)) {
            cashback = handleSpendingThresholdCashback(commerciant, transactionAmount, userPlan, transactionCurrency, accountCurrency, exchangeRateManager);
        }

        return cashback;
    }

    private double handleNrOfTransactionsCashback(String commerciant, double transactionAmount, String category) {
        transactionsCount.put(category, transactionsCount.getOrDefault(category, 0) + 1);

        int count = transactionsCount.get(category);
        double cashbackPercentage = 0.0;

        if (category.equals("Food") && count == 2) {
            cashbackPercentage = 0.02;
        } else if (category.equals("Clothes") && count == 5) {
            cashbackPercentage = 0.05;
        } else if (category.equals("Tech") && count == 10) {
            cashbackPercentage = 0.10;
        }

        return cashbackPercentage * transactionAmount;
    }

    private double handleSpendingThresholdCashback(
            String commerciant,
            double transactionAmount,
            String userPlan,
            String transactionCurrency,
            String accountCurrency,
            ExchangeRateManager exchangeRateManager) {

        // Verificăm dacă tranzacția este în RON; dacă nu, o convertim
        double transactionAmountInRON = transactionCurrency.equalsIgnoreCase("RON")
                ? transactionAmount
                : exchangeRateManager.convertCurrency(transactionCurrency, "RON", transactionAmount);

        // Actualizăm suma totală cheltuită în RON pentru comerciant
        spendingTotal.put(commerciant, spendingTotal.getOrDefault(commerciant, 0.0) + transactionAmountInRON);
        double totalSpent = spendingTotal.get(commerciant);
        double cashbackPercentage = 0.0;

        // Verificăm pragurile de cheltuieli
        if (totalSpent >= 500) {
            cashbackPercentage = getPlanPercentage(userPlan, 0.25, 0.5, 0.7);
        } else if (totalSpent >= 300) {
            cashbackPercentage = getPlanPercentage(userPlan, 0.2, 0.4, 0.55);
        } else if (totalSpent >= 100) {
            cashbackPercentage = getPlanPercentage(userPlan, 0.1, 0.3, 0.5);
        }

        // Calculăm cashback-ul în RON
        double cashbackInRON = cashbackPercentage * transactionAmountInRON;

        // Convertim cashback-ul în moneda contului (dacă este necesar)
        double cashbackInAccountCurrency = exchangeRateManager.convertCurrency("RON", accountCurrency, cashbackInRON);
//
//        // Debugging
        System.out.println("[DEBUG] Tranzacție în RON: " + transactionAmountInRON);
        System.out.println("[DEBUG] Total cheltuit în RON: " + totalSpent);
        System.out.println("[DEBUG] Cashback calculat în RON: " + cashbackInRON);
        System.out.println("[DEBUG] Cashback final în moneda contului: " + cashbackInAccountCurrency);

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
