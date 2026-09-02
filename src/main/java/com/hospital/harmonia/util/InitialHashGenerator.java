package com.hospital.harmonia.util;

/**
 * Command-line utility to generate the BCrypt hash of a password.
 * Use this to generate the real hashes for the 3 initial users (RH, Financeiro,
 * CIAU) before running schema.sql, or whenever you need to manually reset a
 * password directly in the database.
 *
 * How to run (inside Eclipse): right-click the file -> Run As -> Java Application.
 * It will ask for the desired password via a program argument, or use "mudar123" by default.
 *
 * Example usage from a terminal (after build):
 *   java -cp target/classes com.hospital.harmonia.util.InitialHashGenerator minhaSenhaForte
 */
public final class InitialHashGenerator {

    private InitialHashGenerator() {
    }

    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "plantao";
        String hash = PasswordUtil.hash(password);
        System.out.println("Password:    " + password);
        System.out.println("BCrypt hash: " + hash);
        System.out.println();
        System.out.println("Copy the hash above into the senha_hash column of the desired user, e.g.:");
        System.out.println("UPDATE usuarios SET senha_hash = '" + hash + "' WHERE nome_usuario = 'ciau.usuario';");
    }
}
