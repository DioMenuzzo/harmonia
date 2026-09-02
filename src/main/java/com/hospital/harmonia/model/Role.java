package com.hospital.harmonia.model;

/**
 * System access roles.
 * RH            -> access to the Cafeteria (Refeitorio) module
 * FINANCEIRO    -> access to the Asset Disposal module
 * CIAU          -> access to all modules (general administrator)
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

    public boolean canAccessCafeteria() {
        return this == RH || this == CIAU;
    }

    public boolean canAccessAssetDisposal() {
        return this == FINANCEIRO || this == CIAU;
    }

    public boolean canAccessPrinters() {
        // Current rule: CIAU controls the printers module.
        // Adjust here if another role also needs access.
        return this == CIAU;
    }
}
