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
    // nome_exibicao, caminho_foto_perfil, ativo) are still the ones actually in
    // the database -- only the Java-side names were translated. Update these
    // SQL strings together with the database migration when that happens.
    private static final String SQL_FIND_BY_USERNAME =
            "SELECT id, nome_usuario, senha_hash, perfil, nome_exibicao, caminho_foto_perfil, ativo " +
            "FROM usuarios WHERE nome_usuario = ? AND ativo = TRUE";

    // Unlike findByUsername (used for login), this brings back EVERY account
    // (active or not) -- it feeds the user management screen, where the CIAU
    // admin needs to see and edit all of them.
    private static final String SQL_FIND_ALL =
            "SELECT id, nome_usuario, senha_hash, perfil, nome_exibicao, caminho_foto_perfil, ativo " +
            "FROM usuarios ORDER BY nome_exibicao";

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

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("nome_usuario"));
        u.setPasswordHash(rs.getString("senha_hash"));
        u.setRole(Role.valueOf(rs.getString("perfil")));
        u.setDisplayName(rs.getString("nome_exibicao"));
        u.setProfilePicturePath(rs.getString("caminho_foto_perfil"));
        u.setActive(rs.getBoolean("ativo"));
        return u;
    }
}
