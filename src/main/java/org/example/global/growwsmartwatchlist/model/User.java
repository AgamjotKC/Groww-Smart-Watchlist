package org.example.global.growwsmartwatchlist.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime lastSeenAt;

    public User() {}

    public User(Long id, String username, String email, LocalDateTime lastSeenAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.lastSeenAt = lastSeenAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
