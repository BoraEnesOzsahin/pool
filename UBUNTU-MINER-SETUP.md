# 🐧 Ubuntu Mining Device Setup Guide

## Quick Setup for SRBMiner on Ubuntu Terminal

### Step 1: Download SRBMiner

```bash
cd ~
wget https://github.com/doktor83/SRBMiner-Multi/releases/download/2.6.6/SRBMiner-Multi-2-6-6-Linux.tar.gz
tar -xvf SRBMiner-Multi-2-6-6-Linux.tar.gz
cd SRBMiner-Multi-2-6-6
```

### Step 2: Find Your Windows Proxy IP

**On your Windows machine**, run:
```powershell
ipconfig
```

Look for **IPv4 Address** (example: `192.168.1.100`)

### Step 3: Test Connection to Proxy

```bash
# Replace 192.168.1.100 with your Windows IP
nc -zv 192.168.1.100 3333
```

**Expected output:**
```
Connection to 192.168.1.100 3333 port [tcp/*] succeeded!
```

If this fails, see **Troubleshooting** section below.

### Step 4: Create Miner Configuration

```bash
nano config.txt
```

**Paste this** (replace values):
```
ALGO = randomx
POOL = 192.168.1.100:3333
WALLET = yourAntpoolSubaccount.w-abc123
PASSWORD = x
CPU_THREADS = 4
DISABLE_GPU = true
```

**Important replacements:**
- `192.168.1.100` → Your Windows machine IP
- `yourAntpoolSubaccount` → Your actual AntPool subaccount
- `w-abc123` → Worker name (can be anything, e.g., `w-ubuntu-miner`)

Save: `Ctrl+O`, Enter, `Ctrl+X`

### Step 5: Run the Miner

```bash
./SRBMiner-MULTI --config config.txt
```

**You should see:**
```
[INFO] Connected to pool: 192.168.1.100:3333
[INFO] Difficulty: 120001
[INFO] Mining...
```

### Alternative: Command Line (No Config File)

```bash
./SRBMiner-MULTI --algorithm randomx \
  --pool 192.168.1.100:3333 \
  --wallet yourAntpoolSubaccount.w-ubuntu-miner \
  --password x \
  --cpu-threads-intensity 2 \
  --disable-gpu
```

---

## 🔍 Verify Everything is Working

### On Ubuntu (Mining Device):

```bash
# Check miner output for:
# - "Connected to pool"
# - "Accepted shares" (after a few minutes)
# - Hashrate display
```

### On Windows (Proxy Server):

Check the proxy console output for:
```
[INFO] New miner connected from /192.168.1.50:54321
[INFO] Forwarding share to upstream pool
```

### Check Status via API:

**From Ubuntu:**
```bash
curl http://192.168.1.100:8081/api/proxy/status
```

**Expected response:**
```json
{
  "status": "running",
  "active_connections": 1,
  "listening_on": "0.0.0.0:3333",
  "upstream_pool": "xmr.antpool.com:9005"
}
```

---

## 🛠️ Troubleshooting

### Problem: `nc: connect to 192.168.1.100 port 3333 (tcp) failed: Connection refused`

**Solution 1: Check Windows Firewall**

On Windows, run:
```powershell
New-NetFirewallRule -DisplayName "Stratum Proxy" -Direction Inbound -LocalPort 3333 -Protocol TCP -Action Allow
```

**Solution 2: Verify Proxy is Running**

On Windows, check:
```powershell
Get-NetTCPConnection -LocalPort 3333
# Should show: State=Listen
```

**Solution 3: Check Proxy Configuration**

Edit `c:\Users\a\Desktop\pool\src\main\resources\application.properties`:
```properties
stratum.server.host=0.0.0.0  # NOT 127.0.0.1
```

If changed, rebuild and restart:
```powershell
.\build-proxy.bat
.\run-proxy.bat
```

### Problem: Miner connects but no shares accepted

**Check 1: Verify AntPool credentials**

Test with AntPool directly first:
```bash
./SRBMiner-MULTI --algorithm randomx \
  --pool xmr.antpool.com:9005 \
  --wallet yourAntpoolSubaccount.w-test \
  --password x
```

If this doesn't work, your AntPool credentials are wrong.

**Check 2: Monitor proxy logs**

On Windows, watch the proxy console for error messages.

### Problem: `nc: command not found`

Install netcat:
```bash
sudo apt update
sudo apt install netcat-openbsd
```

Or use telnet:
```bash
sudo apt install telnet
telnet 192.168.1.100 3333
```

### Problem: Permission denied when running SRBMiner

```bash
chmod +x SRBMiner-MULTI
./SRBMiner-MULTI --help
```

---

## 📊 Monitor Your Mining

### Real-time Stats on Ubuntu:

SRBMiner shows:
- Current hashrate (H/s)
- Accepted/Rejected shares
- Pool difficulty
- Uptime

### Check Earnings on AntPool:

1. Visit: https://www.antpool.com/
2. Login with your account
3. Navigate to: Workers → Your subaccount
4. Look for worker: `w-ubuntu-miner` (or whatever you named it)

### Use Worker Management API (Optional):

Create worker via API:
```bash
curl -X POST http://192.168.1.100:8080/api/workers \
  -H "Content-Type: application/json" \
  -d '{
    "subaccount": "yourAntpoolSubaccount",
    "workerName": "ubuntu-miner",
    "prefix": "w"
  }'
```

---

## 🚀 Tips for Best Performance

### 1. Optimize CPU Threads

```
CPU_THREADS = 4
CPU_THREADS_INTENSITY = 2
CPU_AFFINITY = 0,1,2,3
```

Start with `CPU_THREADS = 4` and `CPU_THREADS_INTENSITY = 2`, adjust based on hashrate vs system responsiveness.

### 2. Run in Background (tmux/screen)

**Using tmux:**
```bash
sudo apt install tmux
tmux new -s miner
./SRBMiner-MULTI --config config.txt
# Press Ctrl+B, then D to detach
# Reconnect: tmux attach -t miner
```

**Using screen:**
```bash
sudo apt install screen
screen -S miner
./SRBMiner-MULTI --config config.txt
# Press Ctrl+A, then D to detach
# Reconnect: screen -r miner
```

### 3. Auto-start on Boot (systemd)

Create service file:
```bash
sudo nano /etc/systemd/system/srbminer.service
```

```ini
[Unit]
Description=SRBMiner Monero Mining
After=network.target

[Service]
Type=simple
User=YOUR_USERNAME
WorkingDirectory=/home/YOUR_USERNAME/SRBMiner-Multi-2-6-6
ExecStart=/home/YOUR_USERNAME/SRBMiner-Multi-2-6-6/SRBMiner-MULTI --config config.txt
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable srbminer
sudo systemctl start srbminer
sudo systemctl status srbminer
```

---

## 📝 Quick Command Reference

```bash
# Download miner
wget https://github.com/doktor83/SRBMiner-Multi/releases/download/2.6.6/SRBMiner-Multi-2-6-6-Linux.tar.gz

# Extract
tar -xvf SRBMiner-Multi-2-6-6-Linux.tar.gz

# Test connection to proxy
nc -zv 192.168.1.100 3333

# Run miner
cd SRBMiner-Multi-2-6-6
./SRBMiner-MULTI --config config.txt

# Check proxy status
curl http://192.168.1.100:8081/api/proxy/status

# Monitor logs (if using tmux)
tmux attach -t miner

# Stop miner
Ctrl+C (or: pkill SRBMiner-MULTI)
```

---

## ✅ Complete Setup Checklist

- [ ] Windows proxy running on port 3333
- [ ] Windows firewall allows port 3333
- [ ] Ubuntu can ping Windows IP
- [ ] Ubuntu can connect to port 3333 (`nc -zv`)
- [ ] SRBMiner downloaded and extracted
- [ ] `config.txt` created with correct IP and credentials
- [ ] Miner shows "Connected to pool"
- [ ] Miner shows accepted shares
- [ ] Worker visible on AntPool dashboard

**If all checked, you're mining! 🎉**
