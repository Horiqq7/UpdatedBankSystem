package org.poo.bank.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reprezinta o tranzactie efectuata in sistemul bancar
 */
public final class Transaction {
    private final int timestamp;
    private final String description;
    private final String senderIBAN;
    private final String receiverIBAN;
    private final double amount;
    private final String currency;
    private final String transferType;
    private final String card; // Numărul cardului
    private final String cardHolder; // Deținătorul cardului
    private final String transactionType;
    private final String commerciant;
    private final List<String> involvedAccounts;
    private final String error;
    private final String newPlanType;  // Adăugăm câmpul newPlanType

    /**
     * Constructor pentru crearea unei tranzactii.
     *
     * @param timestamp Timestamp-ul tranzactiei.
     * @param description Descrierea tranzactiei.
     * @param senderIBAN IBAN-ul expeditorului.
     * @param receiverIBAN IBAN-ul destinatarului.
     * @param amount Suma tranzactionata.
     * @param currency Moneda tranzactiei.
     * @param transferType Tipul transferului.
     * @param card Cardul asociat tranzacției.
     * @param cardHolder Detinatorul cardului.
     * @param commerciant Comerciantul implicat.
     * @param involvedAccounts Conturile implicate.
     * @param error Eroarea asociata unei tranzactii, daca exista.
     * @param transactionType Tipul tranzactiei.
     * @param newPlanType Tipul noului plan (pentru upgrade de plan)
     */
    public Transaction(final int timestamp, final String description, final String senderIBAN,
                       final String receiverIBAN, final double amount, final String currency,
                       final String transferType, final String card, final String cardHolder,
                       final String commerciant, final List<String> involvedAccounts,
                       final String error, final String newPlanType, final String transactionType) {
        this.timestamp = timestamp;
        this.description = description;
        this.senderIBAN = senderIBAN;
        this.receiverIBAN = receiverIBAN;
        this.amount = amount;
        this.currency = currency;
        this.transferType = transferType;
        this.card = card;
        this.cardHolder = cardHolder;
        this.commerciant = commerciant;
        this.involvedAccounts = involvedAccounts;
        this.transactionType = transactionType;
        this.error = error;
        this.newPlanType = newPlanType;  // Setăm noul câmp
    }

    public List<String> getInvolvedAccounts() {
        return involvedAccounts;
    }

    public String getCommerciant() {
        return commerciant;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public String getSenderIBAN() {
        return senderIBAN;
    }

    public String getReceiverIBAN() {
        return receiverIBAN;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTransferType() {
        return transferType;
    }

    public String getCard() {
        return card;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public String getError() {
        return error;
    }

    // Getter pentru newPlanType
    public String getNewPlanType() {
        return newPlanType;
    }

    /**
     * Transforma tranzactia într-un map cu detalii.
     *
     * @return Un map care contine detalii despre fiecare tranzactie.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        switch (transactionType) {
            case "createCard":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("card", card);
                map.put("cardHolder", cardHolder);
                map.put("account", receiverIBAN);
                break;
            case "addAccount", "sendMoneyInsufficientFunds", "deleteAccountError",
                 "payOnlineInsufficientFunds", "payOnlineCardIsFrozen",
                 "checkCardStatusFrozen", "changeInterestRate":
                map.put("timestamp", timestamp);
                map.put("description", description);
                break;
            case "sendMoney":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("senderIBAN", senderIBAN);
                map.put("receiverIBAN", receiverIBAN);
                map.put("amount", amount + " " + currency);
                map.put("transferType", transferType);
                break;
            case "payOnline":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("commerciant", commerciant);
                map.put("amount", amount);
                break;
            case "deleteCard":
                map.put("timestamp", timestamp);
                map.put("card", card);
                map.put("account", senderIBAN);
                map.put("cardHolder", cardHolder);
                map.put("description", description);
                break;
            case "splitPayment":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("currency", currency);
                map.put("involvedAccounts", involvedAccounts);
                map.put("amount", amount);
                break;
            case "splitPaymentError":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("currency", currency);
                map.put("involvedAccounts", involvedAccounts);
                map.put("amount", amount);
                map.put("error", error);
                break;
            case "destroyOneTimeCard", "newOneTimeCard":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("card", card);
                map.put("cardHolder", cardHolder);
                map.put("account", senderIBAN);
                break;
            case "spending'sReportError":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("error", error);
                break;

            case "withdrawSavings":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("amount", amount + " " + currency);
                map.put("senderIBAN", senderIBAN);
                break;

            case "withdrawSavingsError":
                map.put("timestamp", timestamp);
                map.put("description", description);
                break;
            case "upgradePlan":
                map.put("timestamp", timestamp);
                map.put("description", description);
                map.put("accountIBAN", senderIBAN);
                map.put("newPlanType", newPlanType);  // Adăugăm câmpul newPlanType
                break;
            case "cashWithdrawalError":
                map.put("timestamp", timestamp);
                map.put("description", description);
                break;

            default:
                break;
        }
        return map;
    }
}
