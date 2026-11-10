package com.sms.model;

public class User {
    private int id;
    private String username;
    private String role;
    private Integer studentId;

    public User(int id, String username, String role, Integer studentId) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.studentId = studentId;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Integer getStudentId() { return studentId; }
}
