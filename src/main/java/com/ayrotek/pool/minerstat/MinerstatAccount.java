package com.ayrotek.pool.minerstat;

import com.ayrotek.pool.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "minerstat_accounts")
public class MinerstatAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 128, nullable = false)
    private String apiKey;

    @Column(length = 64, nullable = false)
    private String worker;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getWorker() { return worker; }
    public void setWorker(String worker) { this.worker = worker; }
}
