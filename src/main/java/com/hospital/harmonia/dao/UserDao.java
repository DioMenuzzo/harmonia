package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findByUsername(String username);
    void updateProfilePicture(int userId, String picturePath);

    /** All registered users (any role, active or inactive) -- feeds the user management screen. */
    List<User> findAll();

    /** @throws IllegalArgumentException if the new username is already taken by another account */
    void updateUsername(int userId, String newUsername);

    void updatePasswordHash(int userId, String newPasswordHash);
}
