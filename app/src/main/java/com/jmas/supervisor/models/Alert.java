package com.jmas.supervisor.models;

public class Alert {
    private String tabletId;
    private String message;
    private long timestamp;
    private String type;
    private boolean resolved;

    public Alert(String tabletId, String message, long timestamp, String type) {
        this.tabletId = tabletId;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.resolved = false;
    }

    // Getters y Setters
    public String getTabletId() { return tabletId; }
    public void setTabletId(String tabletId) { this.tabletId = tabletId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
