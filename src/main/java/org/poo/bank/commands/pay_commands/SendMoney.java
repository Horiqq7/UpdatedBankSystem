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
import java.util.Collections;

public final class SendMoney {
    private final List<User> users;

    public SendMoney(final List<User> users) {
        this.users = users;
    }

    /**
     * Transfer bani de la un utilizator la altul.
     * Aceasta metoda verifica daca contul expeditorului si destinatarului exista,
     * daca expeditorul are suficiente fonduri si se efectueaza tranzactia.
     * In cazul in care monedele diferite sunt implicate, se efectueaza conversia valutara.
     * Daca tranzactia nu poate fi realizata, sunt adaugate erori corespunzatoare in
     * lista de iesire.
     *
     * @param command comanda care contine detaliile tranzactiei.
     * @return lista de erori, daca exista, sau lista goala daca tranzactia a avut succes
     */
    public List<Map<String, Object>> sendMoney(final CommandInput command) {
        List<Map<String, Object>> output = new ArrayList<>();
        String senderIBAN = command.getAccount();
        String receiverIBAN = command.getReceiver();
        double amount = command.getAmount();
        String description = command.getDescription();
        int timestamp = command.getTimestamp();

        // Verificăm dacă suma este validă
        if (amount <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Invalid amount");
            output.add(error);
            return output;
        }

        // Găsim contul expeditorului
        Account senderAccount = null;
        User senderUser = null;
        for (User user : users) {
            senderAccount = user.getAccountByIBAN(senderIBAN);
            if (senderAccount != null) {
                senderUser = user;
                break;
            }
        }

        if (senderAccount == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Sender account not found");
            output.add(error);
            return output;
        }

        // Găsim contul destinatarului
        Account receiverAccount = null;
        User receiverUser = null;
        for (User user : users) {
            receiverAccount = user.getAccountByIBAN(receiverIBAN);
            if (receiverAccount != null) {
                receiverUser = user;
                break;
            }
        }

        if (receiverAccount == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Receiver account not found");
            output.add(error);
            return output;
        }

        // Verificăm dacă expeditorul are suficiente fonduri
        if (senderAccount.getBalance() < amount) {
            Transaction insufficientFundsTransaction = new Transaction(
                    timestamp,
                    "Insufficient funds",
                    senderIBAN,
                    receiverIBAN,
                    amount,
                    senderAccount.getCurrency(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "sendMoneyInsufficientFunds"
            );
            senderUser.addTransaction(insufficientFundsTransaction);
            senderAccount.addTransaction(insufficientFundsTransaction);

            Map<String, Object> error = new HashMap<>();
            error.put("description", "Insufficient funds in sender account");
            output.add(error);
            return output;
        }

        // Calculăm suma convertită dacă monedele sunt diferite
        double convertedAmount = amount;
        double amountRON = amount;

        // Dacă monedele sunt diferite, efectuăm conversia
        if (!senderAccount.getCurrency().equalsIgnoreCase(receiverAccount.getCurrency())) {
            try {
                // Convertim suma în moneda destinatarului, folosind direct monedele implicate
                convertedAmount = ExchangeRateManager.getInstance().convertCurrency(
                        senderAccount.getCurrency(),
                        receiverAccount.getCurrency(),
                        amount
                );

                // Convertim suma în RON pentru calcularea comisionului, dacă expeditorul nu folosește RON
                if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                    amountRON = ExchangeRateManager.getInstance().convertCurrency(
                            senderAccount.getCurrency(),
                            "RON",
                            amount
                    );
                } else {
                    // Dacă expeditorul folosește deja RON, suma în RON este aceeași cu suma inițială
                    amountRON = amount;
                }
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("description", "Exchange rates not available");
                output.add(error);
                return output;
            }
        }

        double comision = 0.0;
        double comisionInAccountCurrency = 0.0; // Inițializăm variabila pentru comisionul în moneda contului

        try {
            // Verificăm dacă expeditorul are planul "silver"
            if ("silver".equalsIgnoreCase(senderUser.getPlan())) {
                // Convertim suma în RON pentru calculul comisionului, dacă moneda nu este deja RON
                if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                    amountRON = ExchangeRateManager.getInstance().convertCurrency(
                            senderAccount.getCurrency(),
                            "RON",
                            amount
                    );
                } else {
                    amountRON = amount; // Dacă moneda este deja RON, nu facem conversia
                }

                // Aplicăm comisionul dacă suma în RON depășește 500
                if (amountRON > 500) {
                    comision = 0.1 / 100 * amountRON; // Calculăm comisionul ca 0.1% din suma în RON
                    System.out.println("Comision aplicat: " + comision + " RON");
                }

                // Convertim comisionul din RON în moneda contului expeditorului, dacă este necesar
                if (!senderAccount.getCurrency().equalsIgnoreCase("RON")) {
                    comisionInAccountCurrency = ExchangeRateManager.getInstance().convertCurrency(
                            "RON",
                            senderAccount.getCurrency(),
                            comision
                    );
                } else {
                    comisionInAccountCurrency = comision; // Dacă moneda este deja RON, comisionul rămâne neschimbat
                }

                System.out.println("Comisionul în moneda contului expeditorului (" + senderAccount.getCurrency() + "): " + comisionInAccountCurrency);

                // Retragem comisionul din contul expeditorului
                senderAccount.withdrawFunds(comisionInAccountCurrency);
            } else {
                // Nu se aplică comision dacă planul nu este "silver"
                System.out.println("Niciun comision nu a fost aplicat, planul expeditorului: " + senderUser.getPlan());
            }
        } catch (IllegalArgumentException e) {
            // Gestionăm cazul în care nu există cursuri de schimb valabile
            Map<String, Object> error = new HashMap<>();
            error.put("description", "Exchange rates not available for commission calculation");
            output.add(error);
            return output;
        }



        // Efectuăm tranzacțiile
        senderAccount.withdrawFunds(amount);
        receiverAccount.addFunds(convertedAmount);

        // Creăm tranzacțiile pentru expeditor și destinatar
        Transaction senderTransaction = new Transaction(
                timestamp,
                description,
                senderIBAN,
                receiverIBAN,
                Double.parseDouble(String.format("%.14f", amount)),
                senderAccount.getCurrency(),
                "sent",
                null,
                null,
                null,
                null,
                null,
                null,
                "sendMoney"
        );

        Transaction receiverTransaction = new Transaction(
                timestamp,
                description,
                senderIBAN,
                receiverIBAN,
                Double.parseDouble(String.format("%.14f", convertedAmount)),
                receiverAccount.getCurrency(),
                "received",
                null,
                null,
                null,
                null,
                null,
                null,
                "sendMoney"
        );

        senderUser.addTransaction(senderTransaction);
        senderAccount.addTransaction(senderTransaction);

        receiverUser.addTransaction(receiverTransaction);
        receiverAccount.addTransaction(receiverTransaction);

        // Adăugăm mesaj de debug
        System.out.println("Suma trimisă inițial: " + amount + " " + senderAccount.getCurrency());
        System.out.println("Comisionul aplicat: " + comision + " RON");
        System.out.println("Suma finală trimisă după comision: " + (convertedAmount) + " " + receiverAccount.getCurrency());

        return Collections.emptyList();
    }
}