package com.osman.vssfx.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Vehicle {
    private long vehicleId;
    private long tenantId;
    private Long customerId;            // nullable
    private String publicId;            // UUID string (nullable olabilir)

    private String plateNo;
    private String vinNo;               // nullable
    private String make;
    private String model;
    private int modelYear;              // 0 => unknown gibi düşünebilirsin
    private String colour;              // nullable
    private long currentKm;

    private VehicleStatus status = VehicleStatus.ACTIVE;
    private String notes;               // nullable
    private LocalDate serviceEntryDate; // nullable
    private LocalDateTime createdAt;    // nullable

    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }

    public long getTenantId() { return tenantId; }
    public void setTenantId(long tenantId) { this.tenantId = tenantId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }

    public String getVinNo() { return vinNo; }
    public void setVinNo(String vinNo) { this.vinNo = vinNo; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getModelYear() { return modelYear; }
    public void setModelYear(int modelYear) { this.modelYear = modelYear; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }

    public long getCurrentKm() { return currentKm; }
    public void setCurrentKm(long currentKm) { this.currentKm = currentKm; }

    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = (status == null) ? VehicleStatus.ACTIVE : status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getServiceEntryDate() { return serviceEntryDate; }
    public void setServiceEntryDate(LocalDate serviceEntryDate) { this.serviceEntryDate = serviceEntryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /* optional ama çok iş görür: TableView debug/log */
    @Override public String toString() {
        return "Vehicle{id=" + vehicleId + ", plate=" + plateNo + ", make=" + make + ", model=" + model + "}";
    }

    /* optional: publicId varsa onu identity kabul etmek mantıklı */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle other)) return false;
        if (publicId != null && other.publicId != null) return publicId.equals(other.publicId);
        return vehicleId == other.vehicleId;
    }

    @Override public int hashCode() {
        return Objects.hash(publicId != null ? publicId : vehicleId);
    }
}
