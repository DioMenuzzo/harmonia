package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.dao.impl.UserDaoImpl;
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
    private final UserDao userDao = new UserDaoImpl();

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
    public List<User> findAllUsers() {
        return userDao.findAll();
    }

    /**
     * Updates the username and/or password of the given user. Only touches
     * whichever field was actually filled in (null/blank = keep unchanged),
     * so the CIAU admin can change just the username, just the password, or
     * both at once from the same form. The new password, if any, is hashed
     * here before reaching the database -- it's never stored in plain text.
     *
     * @throws IllegalArgumentException if the new username is already taken by another account
     */
    public void updateCredentials(int userId, String newUsername, String newPlainPassword) {
        if (newUsername != null && !newUsername.isBlank()) {
            userDao.updateUsername(userId, newUsername.trim());
        }
        if (newPlainPassword != null && !newPlainPassword.isBlank()) {
            userDao.updatePasswordHash(userId, PasswordUtil.hash(newPlainPassword));
        }
    }
}
