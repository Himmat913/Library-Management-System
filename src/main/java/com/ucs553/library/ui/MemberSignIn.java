package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.AuthService;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.Optional;

// a member who was given a temporary password has to pick their own before getting in
public final class MemberSignIn {

    private MemberSignIn() {
    }

    public static boolean completeFirstLogin(Window owner, AuthService auth, Member member) {
        Dialogs.info(owner, "Welcome, " + member.getName() + ".\nYour password was set by the library, "
                + "so please choose your own now. Type the temporary password as the current one.");
        while (true) {
            Optional<PasswordDialog.Passwords> answer = new PasswordDialog(owner).showAndWait();
            if (answer.isEmpty()) {
                return false;
            }
            PasswordDialog.Passwords passwords = answer.get();
            try {
                if (Arrays.equals(passwords.current(), passwords.replacement())) {
                    throw new LibraryException("Choose a password that is different from the temporary one");
                }
                auth.changeMemberPassword(member, passwords.current(), passwords.replacement());
                return true;
            } catch (LibraryException e) {
                Dialogs.error(owner, e.getMessage());
            } finally {
                Arrays.fill(passwords.current(), ' ');
                Arrays.fill(passwords.replacement(), ' ');
            }
        }
    }
}
