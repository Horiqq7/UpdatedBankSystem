package org.poo.bank.commands.account_commands;

import org.poo.bank.account.Account;
import org.poo.fileio.CommandInput;
import org.poo.bank.user.User;

import java.util.List;

public final class SetMinimumBalance {
    private final List<User> users;

    public SetMinimumBalance(final List<User> users) {
        this.users = users;
    }

    /**
     * Seteaza soldul minim pentru un cont existent pe baza comenzii primite.
     * Se cauta contul utilizatorului pe baza IBAN-ului și actualizăm soldul minim.
     *
     * @param command Comanda care contine IBAN-ul si noul sold minim.
     */
    public void setMinimumBalance(final CommandInput command) {
        String iban = command.getAccount();
        double minimumBalance = command.getAmount();

        for (User user : users) {
            Account account = user.getAccountByIBAN(iban);
            if (account != null) {
                account.setMinimumBalance(minimumBalance);
                return;
            }
        }
    }
}
