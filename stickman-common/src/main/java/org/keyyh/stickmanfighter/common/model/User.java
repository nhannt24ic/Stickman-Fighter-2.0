package org.keyyh.stickmanfighter.common.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "ranking_score")
    private int rankingScore;

    // Constructors, Getters, and Setters
    public User() {}

    // Bôi đen tên class -> Chuột phải -> Generate... -> Getters and Setters -> Chọn tất cả
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getRankingScore() { return rankingScore; }
    public void setRankingScore(int rankingScore) { this.rankingScore = rankingScore; }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username='" + username + '\'' + ", displayName='" + displayName + '\'' + '}';
    }
}