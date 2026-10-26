package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findByUsername(String username);
    void updateProfilePicture(int userId, String picturePath);

    /** All registered users (any role, active or inactive) -- feeds the user management screen. */
    List<User> findAll();

    /** Registers a new account, active by default. @throws IllegalArgumentException if the username is already taken */
    User save(User user);

    void deactivate(int userId);
    void activate(int userId);

    /** Updates which modules this account can open -- see User.isAccessX / Role's class comment. */
    void updatePermissions(int userId, boolean accessCafeteria, boolean accessAgendaTelefonica,
                            boolean accessTechnicalSupport);

    /** @throws IllegalArgumentException if the new username is already taken by another account */
    void updateUsername(int userId, String newUsername);

    void updatePasswordHash(int userId, String newPasswordHash);

    /**
     * Updates username and/or password hash for one user as a single atomic
     * operation: either both changes are saved or neither is (see
     * UserService.updateCredentials -- this is what it calls instead of
     * updateUsername()/updatePasswordHash() one after the other, which could
     * leave a user with a new username but the old password if the second
     * call failed after the first had already committed). Pass null for
     * whichever field should stay unchanged.
     *
     * @throws IllegalArgumentException if the new username is already taken by another account
     */
    void updateCredentials(int userId, String newUsername, String newPasswordHash);
}
