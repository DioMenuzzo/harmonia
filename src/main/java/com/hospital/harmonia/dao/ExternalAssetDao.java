package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.AssetDisposalReport;

import java.util.Optional;

/**
 * Queries the EXTERNAL database (the hospital's existing asset management
 * system) to bring in the asset information that will be used in the report.
 *
 * IMPORTANT: adjust the SQL query to match the real schema of the hospital's
 * asset database (table/column names below are an EXAMPLE).
 * If the external system isn't PostgreSQL, change the driver/JDBC URL in
 * DatabaseConfig.getExternalConnection() and add the corresponding driver
 * dependency in pom.xml (e.g. Oracle ojdbc, SQL Server mssql-jdbc, etc).
 */
public interface ExternalAssetDao {
    Optional<AssetDisposalReport> findAssetByTag(String assetTagNumber);
}
