package org.poo.bank.account;

import org.poo.bank.cards.Card;
import org.poo.bank.transaction.Transaction;
import org.poo.bank.user.User;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Account {
    private final String iban;
    private double balance;
    private double minimumBalance;
    private final String currency;
    private final String type;
    private List<Card> cards;
    private List<Transaction> transactions;
    private double interestRate;
    private double blockedFunds;

    private Account(final AccountBuilder builder) {
        this.iban = builder.iban;
        this.balance = builder.accountBalance;
        this.minimumBalance = builder.accountMinimumBalance;
        this.currency = builder.currency;
        this.type = builder.type;
        this.cards = builder.accountCards != null ? builder.accountCards : new ArrayList<>();
        this.transactions = builder.accountTransactions != null
                ? builder.accountTransactions : new ArrayList<>();
        this.interestRate = builder.accountInterestRate;
        this.blockedFunds = 0;
    }


    public void blockFunds(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to block must be greater than zero");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds to block");
        }
        blockedFunds += amount;  // Actualizăm fondurile blocate
    }


    // Metoda pentru a debloca fonduri
    public void unblockFunds(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to unblock must be greater than zero");
        }
        if (blockedFunds < amount) {
            throw new IllegalArgumentException("Cannot unblock more funds than currently blocked");
        }
        blockedFunds -= amount;  // Deblocăm suma
    }

    // Metoda pentru a obține fondurile blocate
    public double getBlockedFunds() {
        return blockedFunds;
    }

    /**
     * Returnează numărul de tranzacții efectuate cu un comerciant specificat.
     *
     * @param commerciant Numele comerciantului.
     * @return Numărul de tranzacții realizate cu comerciantul respectiv.
     */
    public int getTransactionsCountForCommerciant(String commerciant, boolean successfulOnly) {
        int count = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getCommerciant().equalsIgnoreCase(commerciant) && (successfulOnly ? transaction.isSuccessful() : true)) {
                count++;
            }
        }

        return count;
    }



    /**
     * Calculeaza comisionul pentru o suma specificata.
     *
     * @param amount Suma pentru care se calculeaza comisionul.
     * @return Comisionul calculat.
     */
    public double calculateFee(double amount) {
        return amount * 0.01;
    }

    /**
     * Retrage o suma din cont.
     *
     * @param amount Suma de retras.
     */
    public void withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
        } else {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    /**
     * Adauga o tranzactie in lista tranzactiilor asociate contului.
     *
     * @param transaction tranzactia de adaugat.
     */
    public void addTransaction(final Transaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Seteaza rata dobanzii pentru acest cont.
     *
     * @param newInterestRate Noua rata a dobanzii.
     */
    public void setInterestRate(final double newInterestRate) {
        this.interestRate = newInterestRate;
    }

    /**
     * Returneaza rata dobanzii asociata acestui cont.
     *
     * @return Rata dobanzii.
     */
    public double getInterestRate() {
        return interestRate;
    }

    /**
     * Returneaza lista tranzactiilor asociate contului.
     *
     * @return Lista tranzactiilor.
     */
    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Returneaza iban-ul contului.
     *
     * @return iban-ul contului.
     */
    public String getIban() {
        return iban;
    }

    /**
     * Elimina toate cardurile asociate contului.
     */
    public void removeAllCards() {
        this.cards.clear();
    }

    /**
     * Returneaza suma minima necesara in cont.
     *
     * @return Balanta minima.
     */
    public double getMinimumBalance() {
        return minimumBalance;
    }

    /**
     * Seteaza balanta minima a contului.
     *
     * @param minimumBalance Noua balanta minima.
     */
    public void setMinimumBalance(final double minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    /**
     * Returneaza balanta curenta a contului.
     *
     * @return Balanta contului.
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Seteaza balanta contului.
     *
     * @param balance Noua balanta.
     */
    public void setBalance(final double balance) {
        this.balance = balance;
    }

    /**
     * Returneaza moneda in care opereaza contul.
     *
     * @return Moneda contului.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Returneaza tipul contului.
     *
     * @return Tipul contului.
     */
    public String getType() {
        return type;
    }

    /**
     * Returneaza lista de carduri asociate contului.
     *
     * @return Lista de carduri.
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Construieste o reprezentare sub forma de mapa a contului.
     *
     * @return Mapa care contine detalii despre cont.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("IBAN", iban);
        map.put("balance", balance);
        map.put("currency", currency);
        map.put("type", type);
        map.put("cards", cards.stream().map(Card::toMap).collect(Collectors.toList()));
        return map;
    }

    /**
     * Returneaza cardul care corespunde unui numar de card specificat.
     *
     * @param cardNumber Numarul cardului cautat.
     * @return Cardul corespunzator sau null daca nu exista.
     */
    public Card getCardByNumber(final String cardNumber) {
        for (Card card : cards) {
            if (card.getCardNumber().equals(cardNumber)) {
                return card;
            }
        }
        return null;
    }

    /**
     * Gaseste contul care contine un anumit card, utilizand numarul cardului.
     *
     * @param user Utilizatorul caruia ii apartine contul.
     * @param cardNumber Numarul cardului.
     * @return Contul care contine cardul sau null daca nu exista.
     */
    public static Account findByCardNumber(final User user, final String cardNumber) {
        for (Account account : user.getAccounts()) {
            if (account.getCardByNumber(cardNumber) != null) {
                return account;
            }
        }
        return null;
    }

    /**
     * Genereaza un raport al cheltuielilor in functie de tranzactiile
     * realizate in intervalul specificat.
     *
     * @param startTimestamp Inceputul intervalului.
     * @param endTimestamp Sfarsitul intervalului.
     * @return Mapa cu detalii despre raportul de cheltuieli.
     */
    public Map<String, Object> generateSpendingsReport(final int startTimestamp,
                                                       final int endTimestamp) {
        List<Transaction> filteredTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getTimestamp() >= startTimestamp
                    && t.getTimestamp() <= endTimestamp
                    && "payOnline".equals(t.getTransactionType())) {
                filteredTransactions.add(t);
            }
        }

        Map<String, Double> commerciantsTotals = new HashMap<>();
        for (Transaction t : filteredTransactions) {
            if (t.getCommerciant() != null) {
                String commerciant = t.getCommerciant();
                double amount = t.getAmount();
                commerciantsTotals.put(commerciant,
                        commerciantsTotals.getOrDefault(commerciant, 0.0) + amount);
            }
        }

        List<Map<String, Object>> commerciantsList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : commerciantsTotals.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("commerciant", entry.getKey());
            map.put("total", entry.getValue());
            commerciantsList.add(map);
        }

        List<Map<String, Object>> transactionsList = new ArrayList<>();
        for (Transaction transaction : filteredTransactions) {
            transactionsList.add(transaction.toMap());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("IBAN", iban);
        report.put("balance", balance);
        report.put("currency", currency);
        report.put("transactions", transactionsList);
        report.put("commerciants", commerciantsList);

        return report;
    }

    /**
     * Retrage o suma de bani din cont daca exista suficiente fonduri.
     *
     * @param amount Suma de retras.
     * @throws IllegalArgumentException Daca fondurile sunt insuficiente.
     */
    public void withdrawFunds(final double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            Transaction transaction = new Transaction(
                    0,
                    "Funds withdrawn",
                    iban,
                    null,
                    -amount,
                    currency,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    "withdrawFunds"
            );
            addTransaction(transaction);
        } else {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    /**
     * Adauga o suma de bani in cont.
     *
     * @param amount Suma de adaugat. Trebuie sa fie mai mare decat zero.
     */
    public void addFunds(final double amount) {
        if (amount > 0) {
            this.balance += amount;
            Transaction transaction = new Transaction(
                    0,
                    "Funds added",
                    null,
                    iban,
                    amount,
                    currency,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    "addFunds"
            );
            addTransaction(transaction);
        }
    }

    /**
     * Adauga un card in lista de carduri asociate contului.
     *
     * @param card Cardul care trebuie adaugat.
     */
    public void addCard(final Card card) {
        cards.add(card);
        Transaction transaction = new Transaction(
                0,
                "Card added",
                null,
                iban,
                0,
                currency,
                null,
                card.getCardNumber(),
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "addCard"
        );
        addTransaction(transaction);
    }

    /**
     * Elimina un card din lista de carduri asociate contului.
     *
     * @param card Cardul care trebuie eliminat.
     */
    public void removeCard(final Card card) {
        cards.remove(card);
        Transaction transaction = new Transaction(
                0,
                "Card removed",
                null,
                iban,
                0,
                currency,
                null,
                card.getCardNumber(),
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                "removeCard"
        );
        addTransaction(transaction);
    }

    /**
     * Clasa builder pentru crearea obiectelor de tip Account.
     */
    public static class AccountBuilder {
        private String iban;
        private double accountBalance = 0;
        private double accountMinimumBalance = 0;
        private String currency;
        private String type;
        private List<Card> accountCards;
        private List<Transaction> accountTransactions;
        private double accountInterestRate;

        /**
         * Constructor pentru AccountBuilder.
         *
         * @param iban IBAN-ul contului.
         * @param currency Moneda contului.
         * @param type Tipul contului.
         */
        public AccountBuilder(final String iban, final String currency, final String type) {
            this.iban = iban;
            this.currency = currency;
            this.type = type;
        }

        /**
         * Seteaza balanta initiala pentru cont.
         *
         * @param balance Balanta initiala.
         * @return Instanta actualizata a builder-ului.
         */
        public final AccountBuilder balance(final double balance) {
            this.accountBalance = balance;
            return this;
        }

        /**
         * Seteaza balanta minima necesara pentru cont.
         *
         * @param minimumBalance Balanta minima.
         * @return Instanta actualizata a builder-ului.
         */
        public final AccountBuilder minimumBalance(final double minimumBalance) {
            this.accountMinimumBalance = minimumBalance;
            return this;
        }

        /**
         * Seteaza rata dobanzii pentru cont.
         *
         * @param interestRate Rata dobanzii.
         * @return Instanta actualizata a builder-ului.
         */
        public final AccountBuilder interestRate(final double interestRate) {
            this.accountInterestRate = interestRate;
            return this;
        }

        /**
         * Seteaza lista de carduri asociate contului.
         *
         * @param cards Lista de carduri.
         * @return Instanta actualizata a builder-ului.
         */
        public final AccountBuilder cards(final List<Card> cards) {
            this.accountCards = cards;
            return this;
        }

        /**
         * Seteaza lista de tranzactii asociate contului.
         *
         * @param transactions Lista de tranzactii.
         * @return Instanta actualizata a builder-ului.
         */
        public AccountBuilder transactions(final List<Transaction> transactions) {
            this.accountTransactions = transactions;
            return this;
        }

        /**
         * Construieste un obiect de tip Account utilizand configuratia actuala.
         *
         * @return Un nou obiect Account.
         */
        public Account build() {
            return new Account(this);
        }
    }
}
