package org.poo.bank.user;

import org.poo.bank.account.Account;
import org.poo.bank.transaction.Transaction;
import org.poo.fileio.UserInput;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class User {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final List<Account> accounts = new ArrayList<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final String birthDate;
    private final String occupation;
    private String plan;

    public User(final UserInput input) {
        this.firstName = input.getFirstName();
        this.lastName = input.getLastName();
        this.email = input.getEmail();
        this.birthDate = input.getBirthDate();
        this.occupation = input.getOccupation();

        if ("student".equalsIgnoreCase(this.occupation)) {
            this.plan = "student";
        } else {
            this.plan = "standard";
        }
    }

    /**
     * Returneaza primul cont clasic in moneda specificata.
     * @param currency Moneda cautata.
     * @return Contul clasic gasit sau null daca nu exista.
     */
    public Account getFirstClassicAccountByCurrency(final String currency) {
        for (Account account : accounts) {
            if ("classic".equals(account.getType()) && currency.equals(account.getCurrency())) {
                return account;
            }
        }
        return null;
    }

    /**
     * Setez alias-ul pentru un IBAN specific.
     *
     * @param alias Alias-ul utilizatorului
     * @param iban IBAN-ul asociat alias-ului
     */
    public void setAlias(final String alias, final String iban) {
        aliases.put(alias, iban);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Adaug o tranzactie in lista utilizatorului.
     *
     * @param transaction Tranzactia ce trebuie adaugata
     */
    public void addTransaction(final Transaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Adaug un cont utilizatorului.
     *
     * @param account Contul care trebuie adaugat
     */
    public void addAccount(final Account account) {
        accounts.add(account);
    }

    /**
     * Elimin un cont din lista utilizatorului.
     *
     * @param account Contul care trebuie eliminat
     */
    public void removeAccount(final Account account) {
        accounts.remove(account);
    }

    /**
     * Cauta un cont dupa IBAN.
     *
     * @param iban IBAN-ul cautat
     * @return Contul cu IBAN-ul respectiv sau null daca nu exista
     */
    public Account getAccountByIBAN(final String iban) {
        for (Account account : accounts) {
            if (account.getIban().equals(iban)) {
                return account;
            }
        }
        return null;

    }

    /**
     * Caut un utilizator dupa email.
     *
     * @param users Lista de utilizatori
     * @param email Email-ul cautat
     * @return Utilizatorul care corespunde email-ului sau null
     */
    public static User findByEmail(final List<User> users, final String email) {
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Caut un utilizator dupa numarul cardului.
     *
     * @param users Lista de utilizatori
     * @param cardNumber Numarul cardului cautat
     * @return Utilizatorul care corespunde numarului de card sau null
     */
    public static User findByCardNumber(final List<User> users, final String cardNumber) {
        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getCardByNumber(cardNumber) != null) {
                    return user;
                }
            }
        }
        return null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("email", email);
        map.put("accounts", accounts.stream().map(Account::toMap).collect(Collectors.toList()));
        return map;
    }

    /**
     * Returnează varsta utilizatorului bazata pe data nasterii.
     *
     * @return Varsta in ani
     */
    public int getAge() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate birthDate = LocalDate.parse(this.birthDate, formatter);
        return Period.between(birthDate, LocalDate.now()).getYears();
    }


    public String getBirthDate() {
        return birthDate;
    }

    public String getOccupation() {
        return occupation;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(final String plan) {
        this.plan = plan;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public List<Account> getAccounts() {
        return accounts;
    }
}
