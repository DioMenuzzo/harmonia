package com.hospital.harmonia.model;

/**
 * System access roles.
 *
 * Module access (Refeitório/Agenda Telefônica/Assistência Técnica) USED TO be
 * fully determined by this enum (RH -> Refeitório, FINANCEIRO -> Agenda,
 * CIAU -> everything). It's now a per-user setting instead (see
 * User.isAccessCafeteria/isAccessAgendaTelefonica/isAccessTechnicalSupport,
 * editable per account on the Gerenciar Usuários screen), because a real
 * account doesn't always fit neatly into exactly one module -- someone might
 * need both Refeitório and Agenda, for instance. Role now only labels the
 * account (shown in the users table, e.g. for billing/organizational
 * purposes) and still gates the Gerenciar Usuários screen itself
 * (canManageUsers) -- that one stays role-based and CIAU-only, deliberately
 * NOT exposed as a per-user checkbox: it's the admin screen that hands out
 * every other permission, so letting it be self/other-granted through the
 * same checkbox list it controls would be a privilege-escalation hole.
 *
 * NOTE: the constant names (RH, FINANCEIRO, CIAU) intentionally stay as-is --
 * they must match the literal values already stored in the "perfil" column
 * (and its CHECK constraint) in the database, which still uses the Portuguese
 * names. Rename these together with the database migration when that happens.
 */
public enum Role {
    RH,
    FINANCEIRO,
    CIAU;

    public boolean canManageUsers() {
        // Current rule: only CIAU can change other accounts' username/password
        // or module permissions, and only CIAU can reach this screen at all.
        return this == CIAU;
    }
}
