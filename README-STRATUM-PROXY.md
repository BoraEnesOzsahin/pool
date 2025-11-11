# Stratum Mining Proxy

A high-performance Stratum protocol proxy for cryptocurrency mining. Routes miners to AntPool (or any Stratum-compatible pool) with transparent share forwarding.

## Features

- ✅ **Stratum V1 Protocol Support** - Full JSON-RPC over TCP
- ✅ **Asynchronous I/O** - Non-blocking NIO for high performance
- ✅ **Multi-Miner Support** - Handle hundreds of concurrent connections
- ✅ **Transparent Proxying** - Zero latency, direct forwarding
- ✅ **Real-time Statistics** - Monitor via REST API
- ✅ **Production Ready** - Docker support with health checks

## Quick Start

### Prerequisites
- Java 21+ installed
- Maven 3.9+
- (Optional) Docker for containerized deployment

### Build & Run

#### 1. Clean Build
```powershell
cd c:\Users\a\Desktop\pool
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
.\mvnw.cmd clean package -DskipTests
```

#### 2. Run Locally
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
.\mvnw.cmd spring-boot:run
```

Or run the JAR directly:
```powershell
java -jar target\pool-0.0.1-SNAPSHOT.jar
```

#### 3. Using Gradle (miner-worker-service)
```powershell
cd c:\Users\a\Desktop\pool\miner-worker-service
.\gradlew.bat bootRun
```

## Configuration

### Configuration File: `src/main/resources/application.properties`

#### a) Local Listening Port for Miners
```properties
# Miners connect here
stratum.server.host=0.0.0.0
stratum.server.port=3333
```

#### b) Upstream Pool Configuration
```properties
# Forward shares to this pool
stratum.upstream.host=xmr.antpool.com
stratum.upstream.port=9005
```

#### c) Logging Level
```properties
# Control verbosity
logging.level.root=INFO
logging.level.com.ayrotek.pool.stratum=INFO

# For debugging:
# logging.level.com.ayrotek.pool.stratum=DEBUG
```

### Environment Variables (Docker/Production)
```bash
STRATUM_SERVER_HOST=0.0.0.0
STRATUM_SERVER_PORT=3333
STRATUM_UPSTREAM_HOST=xmr.antpool.com
STRATUM_UPSTREAM_PORT=9005
LOGGING_LEVEL_ROOT=INFO
```

## Verification Checklist

### ✅ 1. Expected Console Output

When proxy starts successfully:
```
╔════════════════════════════════════════════════════════════╗
║   STRATUM PROXY SERVER STARTED                             ║
╠════════════════════════════════════════════════════════════╣
║  Listening: 0.0.0.0   :3333
║  Upstream:  xmr.antpool.com :9005
╚════════════════════════════════════════════════════════════╝

Started PoolApplication in 3.521 seconds
```

When miner connects:
```
✓ New connection #1: /192.168.1.100:54321 -> Establishing upstream
[miner-1] ✓ Connected to upstream pool xmr.antpool.com:9005
[miner-1] ← mining.subscribe (miner)
[miner-1] ← mining.authorize: yourAntpoolSubaccount.w-abc123
[miner-1] → difficulty set: 10000
[miner-1] → mining.notify (new job)
[miner-1] ← share submitted (total: 1)
```

### ✅ 2. Port Listening Check

#### PowerShell
```powershell
Get-NetTCPConnection -LocalPort 3333 -State Listen
Get-NetTCPConnection -LocalPort 8081 -State Listen
```

**Expected Output:**
```
LocalAddress  LocalPort  RemoteAddress  RemotePort  State    OwningProcess
0.0.0.0       3333       0.0.0.0        0           Listen   12345
0.0.0.0       8081       0.0.0.0        0           Listen   12345
```

#### Linux/WSL
```bash
netstat -tuln | grep -E ':(3333|8081)'
# or
ss -tuln | grep -E ':(3333|8081)'
```

### ✅ 3. Telnet Connection Test

Test Stratum port:
```powershell
telnet localhost 3333
```

Or use PowerShell TCP test:
```powershell
Test-NetConnection -ComputerName localhost -Port 3333
```

**Expected:**
```
ComputerName     : localhost
RemoteAddress    : ::1
RemotePort       : 3333
InterfaceAlias   : Loopback Pseudo-Interface 1
SourceAddress    : ::1
TcpTestSucceeded : True
```

### ✅ 4. HTTP API Health Check

```powershell
curl.exe http://localhost:8081/actuator/health
```

**Expected Response:**
```json
{"status":"UP"}
```

### ✅ 5. Proxy Status Check

```powershell
curl.exe http://localhost:8081/api/proxy/status
```

**Expected Response:**
```json
{
  "proxy_status": "running",
  "active_connections": 0,
  "listening": {
    "host": "0.0.0.0",
    "port": "3333"
  },
  "upstream_pool": {
    "host": "xmr.antpool.com",
    "port": "9005"
  }
}
```

### ✅ 6. Test with netcat (Manual Stratum Commands)

```powershell
# Install netcat for Windows if needed, or use WSL
nc localhost 3333
```

Send Stratum subscribe command:
```json
{"id":1,"method":"mining.subscribe","params":["srbminer/1.0"]}
```

You should receive a response from the upstream pool.

## Docker Deployment

### Build Docker Image
```powershell
cd c:\Users\a\Desktop\pool
.\mvnw.cmd clean package -DskipTests
docker build -t stratum-proxy:latest .
```

### Run Container
```powershell
docker run -d \
  --name stratum-proxy \
  -p 3333:3333 \
  -p 8081:8081 \
  -e STRATUM_UPSTREAM_HOST=xmr.antpool.com \
  -e STRATUM_UPSTREAM_PORT=9005 \
  -e LOGGING_LEVEL_ROOT=INFO \
  stratum-proxy:latest
```

### Using Docker Compose
```powershell
docker-compose up -d
```

### Check Container Health
```powershell
docker ps
docker logs stratum-proxy
```

### Stop Container
```powershell
docker-compose down
# or
docker stop stratum-proxy
```

## Miner Configuration

### SRBMiner Example

Create `config.txt`:
```json
{
  "algorithm": "randomx",
  "pools": [
    {
      "pool": "localhost:3333",
      "wallet": "yourAntpoolSubaccount.w-abc123",
      "password": "x"
    }
  ],
  "cpu_threads": 8
}
```

Run:
```bash
srbminer-multi.exe --config config.txt
```

### XMRig Example

```bash
xmrig.exe -o localhost:3333 \
  -u yourAntpoolSubaccount.w-abc123 \
  -p x \
  -a randomx \
  -t 8
```

## Architecture

```
┌────────────┐         Stratum (3333)        ┌──────────────┐
│            │◄───────────────────────────────┤              │
│   Miner 1  │                                │              │
│  (XMRig)   │──────────────────────────────►│              │
└────────────┘                                │              │
                                              │   Stratum    │
┌────────────┐         Stratum (3333)        │    Proxy     │
│            │◄───────────────────────────────┤  (NIO TCP)   │
│   Miner 2  │                                │              │
│ (SRBMiner) │──────────────────────────────►│              │
└────────────┘                                │              │
                                              └──────┬───────┘
┌────────────┐         HTTP (8081)                  │
│            │◄───────────────────────────────────┐ │
│  Monitor   │                                    │ │
│   (curl)   │──────────────────────────────────►│ │
└────────────┘                                    │ │
                                                  │ │ Stratum
                                                  │ │ (9005)
                                              ┌───▼─▼───────┐
                                              │             │
                                              │   AntPool   │
                                              │  (Upstream) │
                                              │             │
                                              └─────────────┘
```

## Troubleshooting

### Port 3333 Already in Use
```powershell
# Find process using port
Get-NetTCPConnection -LocalPort 3333 | Select-Object OwningProcess
# Kill process
Stop-Process -Id <PID> -Force
```

Or change port in `application.properties`:
```properties
stratum.server.port=3334
```

### Cannot Connect to Upstream Pool
1. Check firewall allows outbound connections to port 9005
2. Verify DNS resolution: `nslookup xmr.antpool.com`
3. Test connectivity: `Test-NetConnection xmr.antpool.com -Port 9005`

### Miner Shows "Connection Refused"
1. Ensure proxy is running: `curl http://localhost:8081/api/proxy/status`
2. Check port binding: `Get-NetTCPConnection -LocalPort 3333`
3. Try connecting from same machine first: `telnet localhost 3333`

### No Shares Forwarded
1. Enable DEBUG logging:
   ```properties
   logging.level.com.ayrotek.pool.stratum=DEBUG
   ```
2. Check miner worker name matches AntPool format: `subaccount.workername`
3. Verify upstream pool is reachable

## Performance Tips

- **Java Heap**: For 100+ miners, use `-Xmx2G -Xms1G`
- **TCP Buffer**: Increase system TCP buffers for high load
- **Logging**: Use INFO level in production (DEBUG is verbose)
- **Connection Pooling**: Each miner = 2 connections (to miner + to pool)

## Security Considerations

- ⚠️ **No TLS**: Stratum protocol is plain TCP (consider VPN for remote miners)
- ⚠️ **No Authentication**: Proxy doesn't validate worker credentials (pool does)
- ✅ **Firewall**: Restrict port 3333 to trusted IPs
- ✅ **Monitoring**: Use HTTP API to detect anomalies

## Supported Pools

Tested with:
- ✅ AntPool (Monero XMR)
- ✅ Any Stratum V1 compatible pool

To change upstream pool, edit `application.properties`:
```properties
# For Ethereum (example)
stratum.upstream.host=eth.pool.com
stratum.upstream.port=4444

# For Bitcoin (example)
stratum.upstream.host=btc.antpool.com
stratum.upstream.port=3333
```

## API Endpoints

### Proxy Status
```
GET http://localhost:8081/api/proxy/status
```

### Health Check
```
GET http://localhost:8081/actuator/health
```

### Spring Boot Actuator (if enabled)
```
GET http://localhost:8081/actuator/metrics
GET http://localhost:8081/actuator/info
```

## License

MIT License

## Support

For issues or questions:
1. Check logs: `logging.level.com.ayrotek.pool.stratum=DEBUG`
2. Test connectivity: `telnet localhost 3333`
3. Verify pool: `Test-NetConnection xmr.antpool.com -Port 9005`

---

**Built with ❤️ for the mining community**
