package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.model.Role;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises AuthService's authentication rules in isolation, with an
 * in-memory fake instead of a real database (see FakeUserDao below).
 * AuthService accepts an injected UserDao through its package-private
 * constructor exactly so it can be tested this way.
 */
class AuthServiceTest {

    private static final String USERNAME = "rh.usuario";
    private static final String PASSWORD = "Sup3rSecret!";

    private AuthService service;

    @BeforeEach
    void setUp() {
        User user = new User(1, USERNAME, PasswordUtil.hash(PASSWORD), Role.RH, "Usuario RH");
        service = new AuthService(new FakeUserDao(user));
    }

    @Test
    void authenticate_succeedsWithTheCorrectPassword() {
        assertTrue(service.authenticate(USERNAME, PASSWORD).isPresent());
    }

    @Test
    void authenticate_failsWithTheWrongPassword() {
        assertFalse(service.authenticate(USERNAME, "wrong-password").isPresent());
    }

    @Test
    void authenticate_failsForAnUnknownUsername() {
        assertFalse(service.authenticate("does.not.exist", PASSWORD).isPresent());
    }

    /** Minimal in-memory stand-in for UserDaoImpl -- no database involved. */
    private static class FakeUserDao implements UserDao {
        private final User user;

        FakeUserDao(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return user.getUsername().equals(username) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public void updateProfilePicture(int userId, String picturePath) {
            user.setProfilePicturePath(picturePath);
        }

        @Override
        public List<User> findAll() {
            return List.of(user);
        }

        @Override
        public void updateUsername(int userId, String newUsername) {
            user.setUsername(newUsername);
        }

        @Override
        public void updatePasswordHash(int userId, String newPasswordHash) {
            user.setPasswordHash(newPasswordHash);
        }

        @Override
        public void updateCredentials(int userId, String newUsername, String newPasswordHash) {
            if (newUsername != null) {
                user.setUsername(newUsername);
            }
            if (newPasswordHash != null) {
                user.setPasswordHash(newPasswordHash);
            }
        }

        // The four methods below were added to UserDao for user registration
        // and the module-permissions screen (UserManagementController) --
        // AuthService itself never calls them, but the fake still has to
        // implement the whole interface. They act on the single in-memory
        // "user" this fake holds, same as updateUsername/updatePasswordHash
        // above, ignoring userId (there is only ever one user here).

        @Override
        public User save(User newUser) {
            throw new UnsupportedOperationException(
                    "FakeUserDao holds a single pre-built user; not exercised by AuthServiceTest.");
        }

        @Override
        public void deactivate(int userId) {
            user.setActive(false);
        }

        @Override
        public void activate(int userId) {
            user.setActive(true);
        }

        @Override
        public void updatePermissions(int userId, boolean accessCafeteria, boolean accessAgendaTelefonica,
                                       boolean accessTechnicalSupport) {
            user.setAccessCafeteria(accessCafeteria);
            user.setAccessAgendaTelefonica(accessAgendaTelefonica);
            user.setAccessTechnicalSupport(accessTechnicalSupport);
        }
    }
}
