package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.dao.impl.UserDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
}
