# Miner Worker Management Service

A minimal Spring Boot service for managing AntPool workers with secure password generation and SRBMiner configuration output.

## Features

- **Worker Management**: Automatic generation of AntPool-format worker IDs (`subaccount.prefix-uuid`)
- **Secure Password Storage**: Argon2id password hashing
- **SRBMiner Config**: REST endpoint for mining software configuration
- **H2 Database**: In-memory database for development (production-ready for PostgreSQL/MySQL)

## Technology Stack

- Java 17
- Spring Boot 3.3.5
- Gradle (Kotlin DSL)
- H2 Database (development)
- Argon2-JVM for password hashing

## Prerequisites

- Java 17 or higher
- Gradle 8.x (included via wrapper)

## Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
app:
  subaccount: yourAntpoolSubaccount  # Your AntPool subaccount name
  workerNamePrefix: w                 # Default prefix for worker names
  defaultPool: antpool                # Pool identifier
```

## Build and Run

### Build the project
```powershell
.\gradlew.bat build
```

### Run the application
```powershell
.\gradlew.bat bootRun
```

The service will start on `http://localhost:8080`

### Run tests
```powershell
.\gradlew.bat test
```

## API Endpoints

### 1. Create Worker

Creates a new worker with auto-generated credentials.

**Request (with your own subaccount):**
```powershell
curl -s -X POST http://localhost:8080/api/workers -H "Content-Type: application/json" -d "{\"subaccount\":\"myAntpoolAccount\",\"prefix\":\"miner\"}"
```

**Request (using default subaccount from config):**
```powershell
curl -s -X POST http://localhost:8080/api/workers -H "Content-Type: application/json" -d "{\"prefix\":\"w\"}"
```

**Request (minimal - uses all defaults):**
```powershell
curl -s -X POST http://localhost:8080/api/workers
```

**Response:**
```json
{
  "worker": "myAntpoolAccount.miner-1a2b3c4d",
  "password": "Xy9@bK#mN2pQ$rTv"
}
```

⚠️ **IMPORTANT**: The plaintext password is only returned once. Save it immediately.

### 2. Get SRBMiner Configuration

Returns ready-to-use configuration for SRBMiner.

**Request:**
```powershell
curl -s "http://localhost:8080/api/miner-config/srbminer?worker=yourAntpoolSubaccount.w-1a2b3c4d&pass=Xy9@bK#mN2pQ$rTv"
```

**Response:**
```json
{
  "ALGO": "randomx",
  "POOL": "stratum+tcp://xmr.antpool.com:9005",
  "WALLET": "yourAntpoolSubaccount.w-1a2b3c4d",
  "PASSWORD": "Xy9@bK#mN2pQ$rTv",
  "CPU_THREADS": "0",
  "DISABLE_GPU": "true"
}
```

## Database Access

H2 Console is available at: `http://localhost:8080/h2-console`

**Connection details:**
- JDBC URL: `jdbc:h2:mem:minerdb`
- Username: `sa`
- Password: (empty)

## Security Notes

### Current Implementation (Development)
- No authentication required
- Passwords stored as Argon2id hashes only
- Plaintext passwords never logged or persisted
- Suitable for development/testing

### Production Recommendations
1. **Database**: Replace H2 with PostgreSQL/MySQL
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/minerdb
       username: your_db_user
       password: your_db_password
     jpa:
       properties:
         hibernate:
           dialect: org.hibernate.dialect.PostgreSQLDialect
   ```
   Add dependency to `build.gradle.kts`:
   ```kotlin
   runtimeOnly("org.postgresql:postgresql")
   ```

2. **Authentication**: Add Spring Security
3. **HTTPS**: Deploy behind reverse proxy (nginx) with TLS
4. **Rate Limiting**: Prevent abuse of worker creation endpoint
5. **Secrets Management**: Use environment variables or secret managers for sensitive config

## Worker Name Format

Workers follow AntPool's naming convention:
- Pattern: `subaccount.workername`
- Example: `myAntpoolAccount.w-1a2b3c4d`
- Auto-generated suffix: First 8 characters of UUID

## Password Generation

- Length: 16 characters
- Character set: `A-Za-z0-9!@#$%^&*`
- Hashing: Argon2id (10 iterations, 64KB memory, 1 parallelism)
- Storage: Only hash persisted; plaintext discarded after response

## Future Enhancements

- [ ] Multi-coin support (configurable pool addresses)
- [ ] Worker status monitoring
- [ ] Hashrate tracking integration
- [ ] Batch worker creation
- [ ] Worker lifecycle management (auto-disable inactive)

## Project Structure

```
src/
├── main/
│   ├── java/com/example/miner/
│   │   ├── MinerWorkerServiceApplication.java
│   │   ├── controller/
│   │   │   ├── WorkerController.java
│   │   │   └── MinerConfigController.java
│   │   ├── entity/
│   │   │   └── WorkerEntity.java
│   │   ├── repository/
│   │   │   └── WorkerRepo.java
│   │   └── service/
│   │       ├── PasswordService.java
│   │       └── WorkerService.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/example/miner/
        └── service/
            └── WorkerServiceTests.java
```

## Troubleshooting

### Port already in use
Change port in `application.yml`:
```yaml
server:
  port: 8081
```

### Database connection issues
Check H2 console at `/h2-console` to verify database state.

### Invalid worker name format
Worker names must match: `^[A-Za-z0-9_-]{1,64}\.[A-Za-z0-9_-]{1,64}$`

## License

MIT License - See LICENSE file for details

## Support

For issues or questions, create an issue in the project repository.
