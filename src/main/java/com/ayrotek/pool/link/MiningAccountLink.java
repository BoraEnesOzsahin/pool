package com.ayrotek.pool.link;

import com.ayrotek.pool.user.User;
import com.ayrotek.pool.user.F2PoolAccount;
import com.ayrotek.pool.minerstat.MinerstatAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "mining_account_links")
public class MiningAccountLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "f2pool_account_id", nullable = false)
    private F2PoolAccount f2poolAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "minerstat_account_id", nullable = false)
    private MinerstatAccount minerstatAccount;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public F2PoolAccount getF2poolAccount() { return f2poolAccount; }
    public void setF2poolAccount(F2PoolAccount f2poolAccount) { this.f2poolAccount = f2poolAccount; }
    public MinerstatAccount getMinerstatAccount() { return minerstatAccount; }
    public void setMinerstatAccount(MinerstatAccount minerstatAccount) { this.minerstatAccount = minerstatAccount; }
}
