package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.UserDao;
import com.hospital.harmonia.model.Role;
import com.hospital.harmonia.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    // Postgres SQLSTATE for "unique_violation" -- see
    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";

    // NOTE: table/column names (usuarios, nome_usuario, senha_hash, perfil,
    // nome_exibicao, caminho_foto_perfil, ativo, acesso_refeitorio,
    // acesso_agenda_telefonica, acesso_assistencia_tecnica) are
    // still the ones actually in the database -- only the Java-side names were
    // translated. Update these SQL strings together with the database migration
    // when that happens.
    private static final String USER_COLUMNS =
            "id, nome_usuario, senha_hash, perfil, nome_exibicao, caminho_foto_perfil, ativo, " +
            "acesso_refeitorio, acesso_agenda_telefonica, acesso_assistencia_tecnica";

    private static final String SQL_FIND_BY_USERNAME =
            "SELECT " + USER_COLUMNS + " FROM usuarios WHERE nome_usuario = ? AND ativo = TRUE";

    // Unlike findByUsername (used for login), this brings back EVERY account
    // (active or not) -- it feeds the user management screen, where the CIAU
    // admin needs to see and edit all of them.
    private static final String SQL_FIND_ALL =
            "SELECT " + USER_COLUMNS + " FROM usuarios ORDER BY nome_exibicao";

    @Override
    public Optional<User> findByUsername(String username) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            log.error("Failed to fetch user by username={}", username, e);
            throw new DataAccessException("Erro ao buscar usuário: " + username, e);
        }
    }

    @Override
    public void updateProfilePicture(int userId, String picturePath) {
        String sql = "UPDATE usuarios SET caminho_foto_perfil = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, picturePath);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            log.info("Profile picture updated for userId={}", userId);
        } catch (SQLException e) {
            log.error("Failed to update profile picture for userId={}", userId, e);
            throw new DataAccessException("Erro ao atualizar foto de perfil do usuário " + userId, e);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            log.error("Failed to list users", e);
            throw new DataAccessException("Erro ao listar usuários", e);
        }
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO usuarios (nome_usuario, senha_hash, perfil, nome_exibicao, ativo, " +
                "acesso_refeitorio, acesso_agenda_telefonica, acesso_assistencia_tecnica) " +
                "VALUES (?, ?, ?, ?, TRUE, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getDisplayName());
            stmt.setBoolean(5, user.isAccessCafeteria());
            stmt.setBoolean(6, user.isAccessAgendaTelefonica());
            stmt.setBoolean(7, user.isAccessTechnicalSupport());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            user.setActive(true);
            log.info("User registered: id={} username={} role={}", user.getId(), user.getUsername(), user.getRole());
            return user;
        } catch (SQLException e) {
            if (SQLSTATE_UNIQUE_VIOLATION.equals(e.getSQLState())) {
                log.warn("Attempt to register duplicate username: {}", user.getUsername());
                throw new IllegalArgumentException(
                        "Já existe um usuário cadastrado com o nome de usuário " + user.getUsername() + ".");
            }
            log.error("Failed to register user (username={})", user.getUsername(), e);
            throw new DataAccessException("Erro ao cadastrar usuário", e);
        }
    }

    @Override
    public void deactivate(int userId) {
        setActive(userId, false);
    }

    @Override
    public void activate(int userId) {
        setActive(userId, true);
    }

    private void setActive(int userId, boolean active) {
        // Soft delete, same pattern as every other module: a deactivated
        // account is never removed, just blocked from logging in (see
        // SQL_FIND_BY_USERNAME's "AND ativo = TRUE" above).
        String sql = "UPDATE usuarios SET ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            log.info("User {}: id={}", active ? "activated" : "deactivated", userId);
        } catch (SQLException e) {
            String message = (active ? "Erro ao ativar usuário " : "Erro ao inativar usuário ") + userId;
            log.error(message, e);
            throw new DataAccessException(message, e);
        }
    }

    @Override
    public void updatePermissions(int userId, boolean accessCafeteria, boolean accessAgendaTelefonica,
                                   boolean accessTechnicalSupport) {
        String sql = "UPDATE usuarios SET acesso_refeitorio = ?, acesso_agenda_telefonica = ?, " +
                "acesso_assistencia_tecnica = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, accessCafeteria);
            stmt.setBoolean(2, accessAgendaTelefonica);
            stmt.setBoolean(3, accessTechnicalSupport);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
            log.info("Module permissions updated for userId={}: refeitorio={} agenda={} assistenciaTecnica={}",
                    userId, accessCafeteria, accessAgendaTelefonica, accessTechnicalSupport);
        } catch (SQLException e) {
            log.error("Failed to update module permissions for userId={}", userId, e);
            throw new DataAccessException("Erro ao atualizar permissões do usuário " + userId, e);
        }
    }

    @Override
    public void updateUsername(int userId, String newUsername) {
        String sql = "UPDATE usuarios SET nome_usuario = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUsername);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            log.info("Username updated for userId={}", userId);
        } catch (SQLException e) {
            if (SQLSTATE_UNIQUE_VIOLATION.equals(e.getSQLState())) {
                log.warn("Attempt to update userId={} to a duplicate username: {}", userId, newUsername);
                throw new IllegalArgumentException(
                        "Já existe um usuário cadastrado com o nome de usuário " + newUsername + ".");
            }
            log.error("Failed to update username for userId={}", userId, e);
            throw new DataAccessException("Erro ao atualizar nome de usuário " + userId, e);
        }
    }

    @Override
    public void updatePasswordHash(int userId, String newPasswordHash) {
        String sql = "UPDATE usuarios SET senha_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            log.info("Password updated for userId={}", userId);
        } catch (SQLException e) {
            log.error("Failed to update password for userId={}", userId, e);
            throw new DataAccessException("Erro ao atualizar senha do usuário " + userId, e);
        }
    }

    @Override
    public void updateCredentials(int userId, String newUsername, String newPasswordHash) {
        if (newUsername == null && newPasswordHash == null) {
            return;
        }
        // One connection, one transaction for both fields: updateUsername()
        // and updatePasswordHash() above each open/commit their own
        // connection, so calling them back to back (the old behavior) could
        // commit the username change and then fail on the password change,
        // leaving the account with a new username but the old password with
        // no way to tell from the screen that only half the change took.
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (newUsername != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE usuarios SET nome_usuario = ? WHERE id = ?")) {
                        stmt.setString(1, newUsername);
                        stmt.setInt(2, userId);
                        stmt.executeUpdate();
                    }
                }
                if (newPasswordHash != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE usuarios SET senha_hash = ? WHERE id = ?")) {
                        stmt.setString(1, newPasswordHash);
                        stmt.setInt(2, userId);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                log.info("Credentials updated for userId={} (username changed={}, password changed={})",
                        userId, newUsername != null, newPasswordHash != null);
            } catch (SQLException e) {
                conn.rollback();
                if (SQLSTATE_UNIQUE_VIOLATION.equals(e.getSQLState())) {
                    log.warn("Attempt to update userId={} to a duplicate username: {}", userId, newUsername);
                    throw new IllegalArgumentException(
                            "Já existe um usuário cadastrado com o nome de usuário " + newUsername + ".");
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Failed to update credentials for userId={}", userId, e);
            throw new DataAccessException("Erro ao atualizar credenciais do usuário " + userId, e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("nome_usuario"));
        u.setPasswordHash(rs.getString("senha_hash"));
        u.setRole(Role.valueOf(rs.getString("perfil")));
        u.setDisplayName(rs.getString("nome_exibicao"));
        u.setProfilePicturePath(rs.getString("caminho_foto_perfil"));
        u.setActive(rs.getBoolean("ativo"));
        u.setAccessCafeteria(rs.getBoolean("acesso_refeitorio"));
        u.setAccessAgendaTelefonica(rs.getBoolean("acesso_agenda_telefonica"));
        u.setAccessTechnicalSupport(rs.getBoolean("acesso_assistencia_tecnica"));
        return u;
    }
}
