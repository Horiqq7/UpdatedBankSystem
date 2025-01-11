
package org.poo.bank.cards;

import java.util.HashMap;
import java.util.Map;

public class Card {
    private String cardNumber;
    private String status;

    public Card(final String cardNumber, final String status) {
        this.cardNumber = cardNumber;
        this.status = status;
    }

    public final void setCardNumber(final String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public final String getCardNumber() {
        return cardNumber;
    }

    public final String getStatus() {
        return status;
    }

    public final void setStatus(final String status) {
        this.status = status;
    }

    public final Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("cardNumber", cardNumber);
        map.put("status", status);
        return map;
    }
}
