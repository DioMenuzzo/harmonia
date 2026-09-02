package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.AssetDisposalReport;

import java.util.List;

public interface AssetDisposalReportDao {
    AssetDisposalReport save(AssetDisposalReport report);
    List<AssetDisposalReport> findAll();
}
