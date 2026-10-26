package com.hospital.harmonia.model;

import java.time.LocalDate;

public class Equipment {

    private Integer id;
    // Type of equipment (Impressora, Notebook, Projetor, Monitor, Desktop/CPU,
    // or free text when "Outro" is chosen) -- see
    // TechnicalSupportController.EQUIPMENT_TYPES.
    private String equipmentType;
    private String model;
    private String brand;
    private String assetTag;
    private String serialNumber;
    private String originSector;
    private String technicalSupport;
    private LocalDate shippedDate;
    private LocalDate returnedDate; // null while still under repair
    private String reportedIssue;
    private String serviceDone;
    // Warranty length in days (30, 60, or whatever the technical support
    // gives -- it varies), not a fixed yes/no. Null means no warranty was
    // informed. See getWarrantyExpirationDate() for the computed end date.
    private Integer warrantyDays;
    // Path of the attached note (PDF) issued by the technical support when
    // the equipment is dropped off -- always available regardless of warranty.
    private String notePath;
    // Soft delete: deactivated instead of permanently deleted, to keep the
    // history -- same pattern as Employee/Meal.
    private boolean active = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public Integer getWarrantyDays() {
        return warrantyDays;
    }

    public void setWarrantyDays(Integer warrantyDays) {
        this.warrantyDays = warrantyDays;
    }

    public String getNotePath() {
        return notePath;
    }

    public void setNotePath(String notePath) {
        this.notePath = notePath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Warranty end date, counted from the RETURN date (when the equipment
     * actually comes back into use), not from the shipment date. Returns
     * null when there's no warranty informed or the equipment hasn't come
     * back yet.
     */
    public LocalDate getWarrantyExpirationDate() {
        if (warrantyDays == null || returnedDate == null) {
            return null;
        }
        return returnedDate.plusDays(warrantyDays);
    }
}
