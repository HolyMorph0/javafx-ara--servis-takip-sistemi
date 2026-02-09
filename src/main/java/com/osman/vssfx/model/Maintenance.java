package com.osman.vssfx.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Maintenance {
    private Long maintId;
    private Long tenantId;
    private Long vehicleId;

    private LocalDate maintDate;
    private String maintType;
    private Integer odometerKm;
    private String description;
    private BigDecimal cost;

    public Long getMaintId() { return maintId; }
    public void setMaintId(Long maintId) { this.maintId = maintId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public LocalDate getMaintDate() { return maintDate; }
    public void setMaintDate(LocalDate maintDate) { this.maintDate = maintDate; }

    public String getMaintType() { return maintType; }
    public void setMaintType(String maintType) { this.maintType = maintType; }

    public Integer getOdometerKm() { return odometerKm; }
    public void setOdometerKm(Integer odometerKm) { this.odometerKm = odometerKm; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
}
