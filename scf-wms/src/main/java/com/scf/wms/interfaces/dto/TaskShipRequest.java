package com.scf.wms.interfaces.dto;

public class TaskShipRequest extends TaskActionRequest {
    private String logisticsProvider;
    private String logisticsProviderName;
    private String trackingNumber;

    public String getLogisticsProvider() {
        return logisticsProvider;
    }

    public void setLogisticsProvider(String logisticsProvider) {
        this.logisticsProvider = logisticsProvider;
    }

    public String getLogisticsProviderName() {
        return logisticsProviderName;
    }

    public void setLogisticsProviderName(String logisticsProviderName) {
        this.logisticsProviderName = logisticsProviderName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}
