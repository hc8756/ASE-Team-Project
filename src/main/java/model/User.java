package dev.coms4156.project.individualproject.model;

import java.util.UUID;

public class User {
    private UUID userId;
    private String username;
    private String email;
    private double budget;
    
    // Constructors
    public User() {
    }
    
    public User(String username, String email, double budget) {
        this.username = username;
        this.email = email;
        this.budget = budget;
    }
    
    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public double getBudget() {
        return budget;
    }
    
    public void setBudget(double budget) {
        this.budget = budget;
    }
}