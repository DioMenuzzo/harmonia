package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.dao.impl.UserDaoImpl;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;

    public AuthService() {
        this(new UserDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see AuthServiceTest). */
    AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates the user. Returns an empty Optional both when the user doesn't
     * exist and when the password is wrong -- intentionally not distinguishing the
     * error message (a security best practice: don't reveal whether the user exists).
     */
    public Optional<User> authenticate(String username, String password) {
        // Never log the password itself, in any form (plain or hashed) -- only
        // the outcome (success/failure) and the username are recorded.
        Optional<User> user = userDao.findByUsername(username)
                .filter(u -> PasswordUtil.verify(password, u.getPasswordHash()));
        if (user.isPresent()) {
            log.info("Successful login: username={}", username);
        } else {
            log.warn("Failed login attempt: username={}", username);
        }
        return user;
    }
}
