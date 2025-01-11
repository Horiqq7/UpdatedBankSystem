package org.poo.bank.exchange_rates;

import org.poo.fileio.ExchangeInput;

/**
 * Reprezinta un curs de schimb valutar.
 */
public final class ExchangeRate {
    private String from;
    private String to;
    private double rate;
    private int timestamp;

    /**
     * Constructorul clasei ExchangeRate.
     *
     * @param exchangeInput Obiect ce contine datele pentru cursul de schimb.
     */
    public ExchangeRate(final ExchangeInput exchangeInput) {
        this.from = exchangeInput.getFrom();
        this.to = exchangeInput.getTo();
        this.rate = exchangeInput.getRate();
        this.timestamp = exchangeInput.getTimestamp();
    }

    /**
     * Returneaza un string cu detaliile cursului de schimb.
     *
     * @return String reprezentand obiectul ExchangeRate.
     */
    @Override
    public String toString() {
        return "ExchangeRate{"
                + "from='" + from
                + '\''
                + ", to='" + to
                + '\''
                + ", rate="
                + rate
                + ", timestamp="
                + timestamp
                + '}';
    }

    public static double convert(String from, String to, double amount) {
        ExchangeRateManager manager = ExchangeRateManager.getInstance();
        return manager.convertCurrency(from, to, amount);
    }


    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(final String to) {
        this.to = to;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(final double rate) {
        this.rate = rate;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final int timestamp) {
        this.timestamp = timestamp;
    }
}
