package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.dao.impl.UserDaoImpl;
import com.hospital.harmonia.model.Role;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final Path PHOTOS_FOLDER = Path.of("fotos_perfil");
    private final UserDao userDao;

    public UserService() {
        this(new UserDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see UserServiceTest). */
    UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Copies the chosen photo into the local photos folder and updates the user's record. */
    public String updateProfilePicture(int userId, Path sourceFile) throws IOException {
        Files.createDirectories(PHOTOS_FOLDER);
        String newName = "usuario_" + userId + "_" + sourceFile.getFileName();
        Path destination = PHOTOS_FOLDER.resolve(newName);
        try {
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save profile picture for userId={}", userId, e);
            throw e;
        }

        userDao.updateProfilePicture(userId, destination.toString());
        return destination.toString();
    }

    /** All registered users -- feeds the user management screen (CIAU only). */
    public List<User> findAll() {
        return userDao.findAll();
    }

    /**
     * Updates the username and/or password of the given user. Only touches
     * whichever field was actually filled in (null/blank = keep unchanged),
     * so the CIAU admin can change just the username, just the password, or
     * both at once from the same form. The new password, if any, is hashed
     * here before reaching the database -- it's never stored in plain text.
     *
     * Both fields are saved through userDao.updateCredentials as a single
     * atomic operation: with two separate DAO calls (the old behavior), a
     * failure on the second one could leave the account with a new username
     * but the old password, reported to the admin as a single failed
     * "update credentials" action that actually half-succeeded.
     *
     * @throws IllegalArgumentException if the new username is already taken by another account
     */
    public void updateCredentials(int userId, String newUsername, String newPlainPassword) {
        String trimmedUsername = (newUsername != null && !newUsername.isBlank()) ? newUsername.trim() : null;
        String newPasswordHash = (newPlainPassword != null && !newPlainPassword.isBlank())
                ? PasswordUtil.hash(newPlainPassword) : null;
        userDao.updateCredentials(userId, trimmedUsername, newPasswordHash);
    }

    /**
     * Registers a new account (CIAU only, enforced by the screen -- see
     * UserManagementController.initialize). The password is hashed here
     * before reaching the database, same as updateCredentials.
     *
     * @throws IllegalArgumentException if a required field is missing or the username is already taken
     */
    public User register(String username, String displayName, String plainPassword, Role role,
                          boolean accessCafeteria, boolean accessAgendaTelefonica, boolean accessTechnicalSupport) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Nome de usuário é obrigatório.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nome de exibição é obrigatório.");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Perfil é obrigatório.");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setDisplayName(displayName.trim());
        user.setPasswordHash(PasswordUtil.hash(plainPassword));
        user.setRole(role);
        user.setAccessCafeteria(accessCafeteria);
        user.setAccessAgendaTelefonica(accessAgendaTelefonica);
        user.setAccessTechnicalSupport(accessTechnicalSupport);
        return userDao.save(user);
    }

    public void deactivate(int userId) {
        userDao.deactivate(userId);
    }

    public void activate(int userId) {
        userDao.activate(userId);
    }

    /** Updates which modules the given account can open (CIAU only, enforced by the screen). */
    public void updateModuleAccess(int userId, boolean accessCafeteria, boolean accessAgendaTelefonica,
                                    boolean accessTechnicalSupport) {
        userDao.updatePermissions(userId, accessCafeteria, accessAgendaTelefonica, accessTechnicalSupport);
    }
}
