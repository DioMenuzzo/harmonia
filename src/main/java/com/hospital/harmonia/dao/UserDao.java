package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.User;

import java.util.Optional;

public interface UserDao {
    Optional<User> findByUsername(String username);
    void updateProfilePicture(int userId, String picturePath);
}
