package org.crmsneakers.crmsneakers.controller;

import org.crmsneakers.crmsneakers.model.User;

public class UserSession {
    private static UserSession instance;
    private User currentUser;
    
    private UserSession() {}
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public void clearSession() {
        this.currentUser = null;
    }
    
    public boolean isAuthenticated() {
        return currentUser != null;
    }
    
    public boolean hasRole(User.UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}