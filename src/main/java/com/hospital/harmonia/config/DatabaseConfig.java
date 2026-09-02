package com.hospital.harmonia.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralizes access configuration for the main database (sysgest_hospital)
 * and the external database queried by the Asset Disposal module.
 *
 * Uses HikariCP as a connection pool: avoids opening/closing a connection on
 * every operation (expensive) and avoids common "connection leak" bugs.
 *
 * IMPORTANT: the two pools (main and external) are initialized LAZILY and
 * INDEPENDENTLY (the "initialization-on-demand holder" idiom, via static
 * nested classes). This is intentional: if the external asset database is
 * misconfigured, unavailable, or simply hasn't been configured yet, that
 * MUST NOT prevent the rest of the system (login, cafeteria, printers) from
 * working -- only the Asset Disposal module is affected, and only when it
 * actually tries to query the external database.
 *
 * Reads configuration from src/main/resources/db.properties. In production,
 * prefer overriding these values with environment variables (DB_URL, DB_USER,
 * DB_PASSWORD) so credentials aren't committed to version control.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final Properties PROPS = loadProperties();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return MainPoolHolder.DATA_SOURCE.getConnection();
    }

    /** Connection to the external asset database, used by the Asset Disposal module. */
    public static Connection getExternalConnection() throws SQLException {
        return ExternalPoolHolder.DATA_SOURCE.getConnection();
    }

    // The JVM only loads (and only runs the static block of) a nested class the
    // first time one of its members is referenced -- that's what makes each
    // pool's initialization independent and on-demand.
    private static final class MainPoolHolder {
        static final HikariDataSource DATA_SOURCE = buildDataSource(
                envOrProperty("DB_URL", PROPS, "db.url"),
                envOrProperty("DB_USER", PROPS, "db.user"),
                envOrProperty("DB_PASSWORD", PROPS, "db.password"),
                Integer.parseInt(PROPS.getProperty("db.pool.maxSize", "10")),
                Integer.parseInt(PROPS.getProperty("db.pool.minIdle", "2"))
        );
    }

    private static final class ExternalPoolHolder {
        static final HikariDataSource DATA_SOURCE = buildDataSource(
                envOrProperty("EXTERNAL_DB_URL", PROPS, "external.db.url"),
                envOrProperty("EXTERNAL_DB_USER", PROPS, "external.db.user"),
                envOrProperty("EXTERNAL_DB_PASSWORD", PROPS, "external.db.password"),
                5,
                1
        );
    }

    private static HikariDataSource buildDataSource(String url, String user, String password,
                                                     int maxSize, int minIdle) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(maxSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(10_000);
        // IMPORTANT: by default Hikari tries to validate a connection AT THE
        // MOMENT the pool is created and kills the process
        // (PoolInitializationException) if it can't. Since
        // MainPoolHolder/ExternalPoolHolder are initialized inside a static
        // block (holder pattern above), that exception becomes an
        // ExceptionInInitializerError on the first failure -- and after that,
        // EVERY subsequent attempt to use the class becomes a
        // NoClassDefFoundError. Neither of those is an Exception (they're
        // Error), so they aren't caught by the catch (Exception e) blocks
        // spread across the controllers, and the affected module (or login,
        // in the case of the main database) stays broken until the program is
        // restarted, with no user-friendly alert on screen.
        // With initializationFailTimeout < 0, Hikari does NOT validate a
        // connection when the pool is created: the first real connection
        // attempt only happens (and can only fail) inside getConnection(), as
        // a normal SQLException -- which is what the DAOs/controllers already
        // know how to handle.
        config.setInitializationFailTimeout(-1);
        log.info("Connection pool created for {} (maxPoolSize={}, minIdle={})",
                maskCredentials(url), maxSize, minIdle);
        return new HikariDataSource(config);
    }

    /** Never log the raw JDBC URL as-is if it happens to carry credentials in the query string. */
    private static String maskCredentials(String url) {
        if (url == null) {
            return "null";
        }
        return url.replaceAll("(?i)(password|user)=[^&]*", "$1=***");
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                log.warn("db.properties not found on the classpath -- relying entirely on environment variables.");
            }
        } catch (IOException e) {
            log.error("Failed to load db.properties", e);
            throw new RuntimeException("Nao foi possivel carregar db.properties", e);
        }
        return props;
    }

    private static String envOrProperty(String envVar, Properties props, String propKey) {
        String value = System.getenv(envVar);
        return (value != null && !value.isBlank()) ? value : props.getProperty(propKey);
    }
}
