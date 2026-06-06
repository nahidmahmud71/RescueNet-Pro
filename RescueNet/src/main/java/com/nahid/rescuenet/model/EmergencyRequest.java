package com.nahid.rescuenet.model;

public class EmergencyRequest {
    private int id;
    private String type;
    private String location;
    private String status;
    private String email;
    private String guardianPhone;

    public EmergencyRequest(int id, String type, String location, String status) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.status = status;
        this.email = "N/A";
        this.guardianPhone = "N/A";
    }

    public EmergencyRequest(int id, String type, String location, String status, String email, String guardianPhone) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.status = status;
        this.email = email;
        this.guardianPhone = guardianPhone;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getEmail() { return email; }
    public String getGuardianPhone() { return guardianPhone; }
}