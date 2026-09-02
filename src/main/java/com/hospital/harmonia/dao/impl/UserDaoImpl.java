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
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    // NOTE: table/column names (usuarios, nome_usuario, senha_hash, perfil,
    // nome_exibicao, caminho_foto_perfil, ativo) are still the ones actually in
    // the database -- only the Java-side names were translated. Update these
    // SQL strings together with the database migration when that happens.
    private static final String SQL_FIND_BY_USERNAME =
            "SELECT id, nome_usuario, senha_hash, perfil, nome_exibicao, caminho_foto_perfil, ativo " +
            "FROM usuarios WHERE nome_usuario = ? AND ativo = TRUE";

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
            throw new DataAccessException("Erro ao buscar usuario: " + username, e);
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
            throw new DataAccessException("Erro ao atualizar foto de perfil do usuario " + userId, e);
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
