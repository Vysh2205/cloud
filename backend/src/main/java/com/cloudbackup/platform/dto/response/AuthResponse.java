package com.cloudbackup.platform.dto.response;

public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;
    private Long storageQuotaBytes;
    private Long usedStorageBytes;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String username, String email, String role,
                        Long storageQuotaBytes, Long usedStorageBytes) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.storageQuotaBytes = storageQuotaBytes;
        this.usedStorageBytes = usedStorageBytes;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getStorageQuotaBytes() {
        return storageQuotaBytes;
    }

    public void setStorageQuotaBytes(Long storageQuotaBytes) {
        this.storageQuotaBytes = storageQuotaBytes;
    }

    public Long getUsedStorageBytes() {
        return usedStorageBytes;
    }

    public void setUsedStorageBytes(Long usedStorageBytes) {
        this.usedStorageBytes = usedStorageBytes;
    }
}
