package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.model.Role;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises UserService's credential-update rules with an in-memory fake
 * instead of a real database (see FakeUserDao below).
 *
 * updateProfilePicture() is deliberately not covered here: it copies a real
 * file on disk to a fixed, non-injectable folder ("fotos_perfil" relative to
 * the working directory), which a plain unit test shouldn't be writing into.
 *
 * The main thing under test is that updateCredentials() goes through
 * UserDao.updateCredentials as a SINGLE atomic call instead of the old
 * updateUsername()+updatePasswordHash() pair -- see
 * updatesBothFieldsThroughOneAtomicCall below and UserDaoImpl.updateCredentials's
 * Javadoc for the bug this replaced (a username change could commit while a
 * failing password change didn't, with a single "update failed" message that
 * didn't reveal the account was left half-changed).
 */
class UserServiceTest {

    private FakeUserDao fakeDao;
    private UserService service;

    @BeforeEach
    void setUp() {
        fakeDao = new FakeUserDao();
        fakeDao.users.add(new User(1, "rh.usuario", PasswordUtil.hash("SenhaAntiga1!"), Role.RH, "Usuario RH"));
        service = new UserService(fakeDao);
    }

    @Test
    void updateCredentials_changesOnlyTheUsernameWhenPasswordIsBlank() {
        service.updateCredentials(1, "rh.novo", "  ");

        assertEquals(1, fakeDao.updateCredentialsCallCount);
        assertEquals("rh.novo", fakeDao.lastNewUsername);
        assertNull(fakeDao.lastNewPasswordHash, "a blank password must be treated as \"keep unchanged\", not hashed");
    }

    @Test
    void updateCredentials_changesOnlyThePasswordWhenUsernameIsBlank() {
        service.updateCredentials(1, null, "SenhaNova1!");

        assertEquals(1, fakeDao.updateCredentialsCallCount);
        assertNull(fakeDao.lastNewUsername, "a blank/null username must be treated as \"keep unchanged\"");
        assertNotEquals("SenhaNova1!", fakeDao.lastNewPasswordHash, "the plain password must never reach the DAO");
        assertTrue(PasswordUtil.verify("SenhaNova1!", fakeDao.lastNewPasswordHash));
    }

    @Test
    void updateCredentials_changesBothFieldsThroughOneAtomicCall() {
        service.updateCredentials(1, "rh.novo", "SenhaNova1!");

        assertEquals(1, fakeDao.updateCredentialsCallCount,
                "both fields must be saved through a single DAO call, not two separate ones");
        assertEquals("rh.novo", fakeDao.lastNewUsername);
        assertTrue(PasswordUtil.verify("SenhaNova1!", fakeDao.lastNewPasswordHash));
    }

    @Test
    void updateCredentials_trimsTheNewUsernameBeforeSaving() {
        service.updateCredentials(1, "  rh.novo  ", null);
        assertEquals("rh.novo", fakeDao.lastNewUsername);
    }

    @Test
    void updateCredentials_treatsNullAndBlankAsNoChangeToEitherField() {
        service.updateCredentials(1, "", "   ");

        assertEquals(1, fakeDao.updateCredentialsCallCount);
        assertNull(fakeDao.lastNewUsername);
        assertNull(fakeDao.lastNewPasswordHash);
    }

    @Test
    void updateCredentials_propagatesADuplicateUsernameAsIllegalArgument() {
        fakeDao.users.add(new User(2, "financeiro.usuario", PasswordUtil.hash("Outra1!"), Role.FINANCEIRO, "Usuario FIN"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateCredentials(1, "financeiro.usuario", null));
    }

    @Test
    void findAll_delegatesToTheDao() {
        assertEquals(fakeDao.users, service.findAll());
    }

    /** Minimal in-memory stand-in for UserDaoImpl -- no database involved. */
    private static class FakeUserDao implements UserDao {
        final List<User> users = new ArrayList<>();
        int updateCredentialsCallCount = 0;
        String lastNewUsername;
        String lastNewPasswordHash;

        @Override
        public Optional<User> findByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }

        @Override
        public void updateProfilePicture(int userId, String picturePath) {
            byId(userId).setProfilePicturePath(picturePath);
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users);
        }

        @Override
        public void updateUsername(int userId, String newUsername) {
            byId(userId).setUsername(newUsername);
        }

        @Override
        public void updatePasswordHash(int userId, String newPasswordHash) {
            byId(userId).setPasswordHash(newPasswordHash);
        }

        @Override
        public void updateCredentials(int userId, String newUsername, String newPasswordHash) {
            updateCredentialsCallCount++;
            lastNewUsername = newUsername;
            lastNewPasswordHash = newPasswordHash;

            // Same conflict rule UserDaoImpl enforces via the database's unique
            // index -- kept here too so updateCredentials_propagatesADuplicate...
            // above exercises the same contract UserService relies on.
            if (newUsername != null) {
                boolean takenByAnotherAccount = users.stream()
                        .anyMatch(u -> u.getId() != userId && newUsername.equals(u.getUsername()));
                if (takenByAnotherAccount) {
                    throw new IllegalArgumentException(
                            "Já existe um usuário cadastrado com o nome de usuário " + newUsername + ".");
                }
            }

            User user = byId(userId);
            if (newUsername != null) {
                user.setUsername(newUsername);
            }
            if (newPasswordHash != null) {
                user.setPasswordHash(newPasswordHash);
            }
        }

        // The four methods below were added to UserDao for user registration
        // and the module-permissions screen (UserManagementController); none
        // of the tests above exercise them, but the fake still has to
        // implement the whole interface -- same list-backed style as the
        // methods above, via the same byId() lookup.

        @Override
        public User save(User newUser) {
            boolean taken = users.stream().anyMatch(u -> u.getUsername().equals(newUser.getUsername()));
            if (taken) {
                throw new IllegalArgumentException(
                        "Já existe um usuário cadastrado com o nome de usuário " + newUser.getUsername() + ".");
            }
            newUser.setId(users.size() + 1);
            users.add(newUser);
            return newUser;
        }

        @Override
        public void deactivate(int userId) {
            byId(userId).setActive(false);
        }

        @Override
        public void activate(int userId) {
            byId(userId).setActive(true);
        }

        @Override
        public void updatePermissions(int userId, boolean accessCafeteria, boolean accessAgendaTelefonica,
                                       boolean accessTechnicalSupport) {
            User user = byId(userId);
            user.setAccessCafeteria(accessCafeteria);
            user.setAccessAgendaTelefonica(accessAgendaTelefonica);
            user.setAccessTechnicalSupport(accessTechnicalSupport);
        }

        private User byId(int userId) {
            return users.stream().filter(u -> u.getId() == userId).findFirst()
                    .orElseThrow(() -> new IllegalStateException("no fake user with id " + userId));
        }
    }
}
