package com.hospital.harmonia.model;

import java.time.LocalDate;

public class Printer {

    private Integer id;
    private String model;
    private String assetTag;
    private String originSector;
    private String technicalSupport;
    private LocalDate shippedDate;
    private LocalDate returnedDate; // null while still under repair
    private String reportedIssue;
    private String serviceDone;
    private boolean underWarranty;
    private String warrantyAttachmentPath; // path of the attached file (PDF/image)

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getOriginSector() {
        return originSector;
    }

    public void setOriginSector(String originSector) {
        this.originSector = originSector;
    }

    public String getTechnicalSupport() {
        return technicalSupport;
    }

    public void setTechnicalSupport(String technicalSupport) {
        this.technicalSupport = technicalSupport;
    }

    public LocalDate getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(LocalDate shippedDate) {
        this.shippedDate = shippedDate;
    }

    public LocalDate getReturnedDate() {
        return returnedDate;
    }

    public void setReturnedDate(LocalDate returnedDate) {
        this.returnedDate = returnedDate;
    }

    public String getReportedIssue() {
        return reportedIssue;
    }

    public void setReportedIssue(String reportedIssue) {
        this.reportedIssue = reportedIssue;
    }

    public String getServiceDone() {
        return serviceDone;
    }

    public void setServiceDone(String serviceDone) {
        this.serviceDone = serviceDone;
    }

    public boolean isUnderWarranty() {
        return underWarranty;
    }

    public void setUnderWarranty(boolean underWarranty) {
        this.underWarranty = underWarranty;
    }

    public String getWarrantyAttachmentPath() {
        return warrantyAttachmentPath;
    }

    public void setWarrantyAttachmentPath(String warrantyAttachmentPath) {
        this.warrantyAttachmentPath = warrantyAttachmentPath;
    }

    public String getStatus() {
        return returnedDate == null ? "Em conserto" : "Retornada";
    }
}
