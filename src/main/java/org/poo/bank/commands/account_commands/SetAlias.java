package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.List;

public final class SetAlias {
    private List<User> users;

    public SetAlias(final List<User> users) {
        this.users = users;
    }

    /**
     * Seteaza alias-ul unui cont specificat de utilizator.
     *
     * @param command Comanda care contine alias-ul, IBAN-ul contului si email-ul utilizatorului.
     *               Daca utilizatorul sau contul nu exista, alias-ul nu va fi setat.
     */
    public void setAlias(final CommandInput command) {
        String alias = command.getAlias();
        String iban = command.getAccount();

        User user = User.findByEmail(users, command.getEmail());
        if (user == null) {
            return;
        }

        Account account = user.getAccountByIBAN(iban);
        if (account == null) {
            return;
        }

        user.setAlias(alias, iban);
    }
}

