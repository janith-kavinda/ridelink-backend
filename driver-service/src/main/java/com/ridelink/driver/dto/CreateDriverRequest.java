package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateDriverRequest {
    @NotBlank(message = "vehiclePlate is required")
    private String vehiclePlate;

    @NotBlank(message = "vehicleModel is required")
    private String vehicleModel;

    @NotBlank(message = "serviceArea is required")
    private String serviceArea;

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
}
