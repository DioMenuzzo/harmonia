package com.hospital.harmonia.model;

public class User {

    private Integer id;
    private String username;
    private String passwordHash;
    private Role role;
    private String displayName;
    private String profilePicturePath;
    private boolean active = true;
    // Per-account module access -- see the comment on Role for why this
    // replaced Role-based access. Each defaults to false (a brand-new account
    // starts with no module until CIAU explicitly grants one).
    private boolean accessCafeteria = false;
    private boolean accessAgendaTelefonica = false;
    private boolean accessTechnicalSupport = false;

    public User() {
    }

    public User(Integer id, String username, String passwordHash, Role role, String displayName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAccessCafeteria() {
        return accessCafeteria;
    }

    public void setAccessCafeteria(boolean accessCafeteria) {
        this.accessCafeteria = accessCafeteria;
    }

    public boolean isAccessAgendaTelefonica() {
        return accessAgendaTelefonica;
    }

    public void setAccessAgendaTelefonica(boolean accessAgendaTelefonica) {
        this.accessAgendaTelefonica = accessAgendaTelefonica;
    }

    public boolean isAccessTechnicalSupport() {
        return accessTechnicalSupport;
    }

    public void setAccessTechnicalSupport(boolean accessTechnicalSupport) {
        this.accessTechnicalSupport = accessTechnicalSupport;
    }
}
