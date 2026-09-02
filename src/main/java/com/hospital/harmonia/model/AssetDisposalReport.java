package com.hospital.harmonia.model;

import java.time.LocalDate;

/**
 * Technical report authorizing an asset's disposal (write-off).
 * The asset identification fields (asset tag, description, value, acquisition
 * date, location) are normally FILLED IN from the external asset database
 * (see ExternalAssetDao), and the report fields (reason, technical opinion,
 * responsible officer) are filled in by the user in this system itself.
 */
public class AssetDisposalReport {

    private Integer id;

    // Coming from the external asset system/database
    private String assetTagNumber;
    private String assetDescription;
    private String location;
    private LocalDate acquisitionDate;
    private String acquisitionValue;

    // Filled in in this system
    private String disposalReason;
    private String technicalOpinion;
    private String technicalOfficer;
    private LocalDate reportDate = LocalDate.now();
    private String status = "PENDENTE"; // PENDENTE, APROVADO, REPROVADO

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAssetTagNumber() {
        return assetTagNumber;
    }

    public void setAssetTagNumber(String assetTagNumber) {
        this.assetTagNumber = assetTagNumber;
    }

    public String getAssetDescription() {
        return assetDescription;
    }

    public void setAssetDescription(String assetDescription) {
        this.assetDescription = assetDescription;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getAcquisitionValue() {
        return acquisitionValue;
    }

    public void setAcquisitionValue(String acquisitionValue) {
        this.acquisitionValue = acquisitionValue;
    }

    public String getDisposalReason() {
        return disposalReason;
    }

    public void setDisposalReason(String disposalReason) {
        this.disposalReason = disposalReason;
    }

    public String getTechnicalOpinion() {
        return technicalOpinion;
    }

    public void setTechnicalOpinion(String technicalOpinion) {
        this.technicalOpinion = technicalOpinion;
    }

    public String getTechnicalOfficer() {
        return technicalOfficer;
    }

    public void setTechnicalOfficer(String technicalOfficer) {
        this.technicalOfficer = technicalOfficer;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
