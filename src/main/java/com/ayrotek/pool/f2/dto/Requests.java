package com.ayrotek.pool.f2.dto;


public class Requests {
public static class MiningUserGet {
public String mining_user_name;
public MiningUserGet() {}
public MiningUserGet(String name) { this.mining_user_name = name; }
}
public static class WorkerList {
public String mining_user_name; // or address (choose one)
public String currency; // Only BTC, BCH, LTC supported by hash_rate module
public WorkerList() {}
public WorkerList(String name, String currency) { this.mining_user_name = name; this.currency = currency; }
}
public static class HashRateHistory {
public String mining_user_name; // or address
public String currency; // BTC, BCH, LTC
public long interval; // seconds, divisible by 600
public long duration; // seconds, up to 30 days
public HashRateHistory() {}
public HashRateHistory(String name, String currency, long interval, long duration) {
this.mining_user_name = name; this.currency = currency; this.interval = interval; this.duration = duration;
}
}
public static class WorkerHistory extends HashRateHistory {
public String worker_name;
public WorkerHistory() {}
public WorkerHistory(String name, String currency, String worker, long interval, long duration) {
super(name, currency, interval, duration); this.worker_name = worker;
}
}
}