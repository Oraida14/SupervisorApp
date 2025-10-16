package com.jmas.supervisor.models;

import java.io.Serializable;

public class Tablet implements Serializable {
    public enum AlertType { NONE, GPS_OFF, OFFLINE, BATTERY_LOW, OTHER }

    private String tabletId;
    private double latitude;
    private double longitude;
    private long lastUpdate;
    private boolean gpsEnabled;
    private boolean online;
    private String status;
    private String address;
    private AlertType alertType = AlertType.NONE;

    public Tablet(String tabletId) {
        this.tabletId = tabletId;
        this.gpsEnabled = true;
        this.online = true;
        this.status = "Activa";
        this.address = "";
    }

    // --- Getters y Setters ---
    public String getTabletId() { return tabletId; }
    public void setTabletId(String tabletId) { this.tabletId = tabletId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(long lastUpdate) { this.lastUpdate = lastUpdate; }

    public boolean isGpsEnabled() { return gpsEnabled; }
    public void setGpsEnabled(boolean gpsEnabled) {
        this.gpsEnabled = gpsEnabled;
        this.status = gpsEnabled ? "Activa" : "GPS Desactivado";
        this.alertType = gpsEnabled ? AlertType.NONE : AlertType.GPS_OFF;
    }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) {
        this.online = online;
        if (!online) this.status = "Desconectada";
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
}
