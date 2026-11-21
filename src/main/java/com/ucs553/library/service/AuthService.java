package com.ucs553.library.service;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.dao.MemberAccountDao;
import com.ucs553.library.dao.MemberDao;
import com.ucs553.library.dao.StaffDao;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.MemberAccount;
import com.ucs553.library.model.Role;
import com.ucs553.library.model.Staff;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
    private static final int MIN_PASSWORD = 6;
    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password".toCharArray());

    private static final String TEMP_ALPHABET = "abcdefghjkmnpqrstuvwxyzACDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TEMP_LENGTH = 8;

    private final StaffDao staff;
    private final MemberDao members;
    private final MemberAccountDao accounts;
    private final SecureRandom random = new SecureRandom();

    public AuthService(StaffDao staff) {
        this(staff, null, null);
    }

    public AuthService(StaffDao staff, MemberDao members, MemberAccountDao accounts) {
        this.staff = staff;
        this.members = members;
        this.accounts = accounts;
    }

    public boolean ensureDefaultAdmin() throws LibraryException {
        if (!staff.findAll().isEmpty()) {
            return false;
        }
        staff.insert(new Staff("Administrator", "admin@library.local", DEFAULT_USER, Role.ADMIN,
                PasswordHasher.hash(DEFAULT_PASSWORD.toCharArray())));
        LOG.warning("Default admin account created, change its password after the first sign in");
        return true;
    }

    public boolean defaultAdminStillUsed() throws DataAccessException {
        Optional<Staff> admin = staff.findByUsername(DEFAULT_USER);
        return admin.isPresent() && PasswordHasher.verify(DEFAULT_PASSWORD.toCharArray(), admin.get().getPasswordHash());
    }

    public Optional<Staff> login(String username, char[] password) throws DataAccessException {
        Optional<Staff> found = staff.findByUsername(username == null ? "" : username.trim());
        if (found.isPresent() && PasswordHasher.verify(password, found.get().getPasswordHash())) {
            LOG.info(found.get().getUsername() + " signed in");
            return found;
        }
        if (found.isEmpty()) {
            PasswordHasher.verify(password, DUMMY_HASH);
        }
        LOG.warning("Failed sign in for \"" + username + "\"");
        return Optional.empty();
    }

    public List<Staff> allStaff() throws DataAccessException {
        return staff.findAll();
    }

    public Staff createStaff(String name, String email, String username, Role role, char[] password)
            throws LibraryException {
        if (username == null || username.isBlank()) {
            throw new LibraryException("Username is required");
        }
        checkPassword(password);
        if (staff.findByUsername(username.trim()).isPresent()) {
            throw new LibraryException("The username " + username.trim() + " is already taken");
        }
        Staff created;
        try {
            created = new Staff(name, email, username, role, PasswordHasher.hash(password));
        } catch (IllegalArgumentException e) {
            throw new LibraryException(e.getMessage());
        }
        for (Staff existing : staff.findAll()) {
            if (existing.getEmail().equals(created.getEmail())) {
                throw new LibraryException("A staff account with that email already exists");
            }
        }
        staff.insert(created);
        LOG.info("Staff account " + created.getUsername() + " created as " + role);
        return created;
    }

    public Staff changePassword(Staff current, char[] oldPassword, char[] newPassword) throws LibraryException {
        if (!PasswordHasher.verify(oldPassword, current.getPasswordHash())) {
            throw new LibraryException("The current password is wrong");
        }
        checkPassword(newPassword);
        Staff updated = new Staff(current.getName(), current.getEmail(), current.getUsername(),
                current.getRole(), PasswordHasher.hash(newPassword));
        updated.setId(current.getId());
        staff.update(updated);
        LOG.info(updated.getUsername() + " changed their password");
        return updated;
    }

    public void deleteStaff(int id, Staff actor) throws LibraryException {
        Staff target = staff.findById(id).orElseThrow(() -> new com.ucs553.library.exception.NotFoundException("Staff", id));
        if (target.getId() == actor.getId()) {
            throw new LibraryException("You cannot delete your own account");
        }
        if (target.isAdmin() && staff.countAdmins() <= 1) {
            throw new LibraryException("The last administrator cannot be deleted");
        }
        staff.delete(id);
        LOG.info("Staff account " + target.getUsername() + " deleted by " + actor.getUsername());
    }

    public Optional<MemberLogin> loginMember(String email, char[] password) throws DataAccessException {
        if (members == null || accounts == null) {
            return Optional.empty();
        }
        Optional<Member> member = members.findByEmail(email == null ? "" : email.trim());
        Optional<MemberAccount> account = member.isPresent() ? accounts.find(member.get().getId()) : Optional.empty();
        if (account.isPresent() && PasswordHasher.verify(password, account.get().passwordHash())) {
            LOG.info(member.get().getEmail() + " signed in");
            return Optional.of(new MemberLogin(member.get(), account.get().mustChange()));
        }
        if (account.isEmpty()) {
            PasswordHasher.verify(password, DUMMY_HASH);
        }
        LOG.warning("Failed sign in for \"" + email + "\"");
        return Optional.empty();
    }

    public boolean hasAccount(int memberId) throws DataAccessException {
        return accounts != null && accounts.find(memberId).isPresent();
    }

    public java.util.Set<Integer> membersWithAccounts() throws DataAccessException {
        return accounts == null ? java.util.Set.of() : accounts.memberIdsWithAccounts();
    }

    // returns the temporary password, which is shown once and has to be changed at the first sign in
    public String createOrResetMemberAccount(int memberId) throws LibraryException {
        requireMemberSupport();
        members.findById(memberId).orElseThrow(() -> new com.ucs553.library.exception.NotFoundException("Member", memberId));
        String temporary = temporaryPassword();
        accounts.save(new MemberAccount(memberId, PasswordHasher.hash(temporary.toCharArray()), true));
        return temporary;
    }

    public void setMemberPassword(int memberId, String passwordHash, boolean mustChange) throws LibraryException {
        requireMemberSupport();
        accounts.save(new MemberAccount(memberId, passwordHash, mustChange));
    }

    public void changeMemberPassword(Member member, char[] oldPassword, char[] newPassword) throws LibraryException {
        requireMemberSupport();
        MemberAccount account = accounts.find(member.getId())
                .orElseThrow(() -> new LibraryException("This member has no account"));
        if (!PasswordHasher.verify(oldPassword, account.passwordHash())) {
            throw new LibraryException("The current password is wrong");
        }
        checkPassword(newPassword);
        accounts.save(new MemberAccount(member.getId(), PasswordHasher.hash(newPassword), false));
        LOG.info(member.getEmail() + " changed their password");
    }

    private String temporaryPassword() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < TEMP_LENGTH; i++) {
            text.append(TEMP_ALPHABET.charAt(random.nextInt(TEMP_ALPHABET.length())));
        }
        return text.toString();
    }

    private void requireMemberSupport() throws LibraryException {
        if (members == null || accounts == null) {
            throw new LibraryException("Member accounts are not available");
        }
    }

    private void checkPassword(char[] password) throws LibraryException {
        if (password == null || password.length < MIN_PASSWORD) {
            throw new LibraryException("Passwords need at least " + MIN_PASSWORD + " characters");
        }
    }
}
