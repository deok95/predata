# PRE(D)ATA - Prediction Market Platform

A decentralized prediction market platform built with Next.js and Spring Boot, enabling users to vote and bet on real-world events.

## Architecture

PRE(D)ATA uses a **monolithic architecture** for simplicity and rapid development:

- **Frontend**: Next.js 15 (React, TypeScript, TailwindCSS)
- **Backend**: Spring Boot 3.2.1 + Kotlin
- **Database**: MariaDB 10.11
- **Cache**: Redis 7
- **Blockchain**: Web3j integration for on-chain settlements

```
Frontend (3000) → Backend (8080) → MariaDB + Redis
```

## Project Structure

```
predata/
├── src/                    # Next.js frontend source
│   ├── app/               # App router pages
│   ├── components/        # React components
│   └── lib/               # Utilities and API clients
├── backend/               # Spring Boot monolithic backend
│   ├── src/main/kotlin/   # Kotlin source code
│   ├── build.gradle.kts   # Gradle build configuration
│   └── README.md          # Backend documentation
├── blockchain/            # Smart contracts (Solidity)
├── public/                # Static assets
├── docker-compose.yml     # Docker orchestration
└── run-local.sh          # Local development script
```

## Quick Start

### Prerequisites

- Node.js 18+ and npm
- Java 17+
- MariaDB 10.11+
- Redis 7+

### Option 1: Local Development (Recommended)

Run backend and infrastructure without Docker:

```bash
# Start MariaDB, Redis, and backend
./run-local.sh

# In a separate terminal, start frontend
npm install
npm run dev
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

### Option 2: Full Docker Setup

Run everything in Docker containers:

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d
```

### Option 3: Frontend Development Only

If you only need to work on the frontend:

```bash
# Make sure backend is running (via run-local.sh or Docker)
npm run dev
```

## Core Features

### 5-Lock Voting System
- Daily 5-vote ticket allocation
- Anti-spam latency checks
- Duplicate vote prevention
- Automatic midnight ticket reset

### Betting System
- Point-based prediction market
- Pessimistic locking for concurrency control
- Real-time pool updates
- Duplicate bet prevention

### Global Persona System
- User classification by country, occupation, age
- Tier system (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND)
- Weighted voting based on tier

### Blockchain Integration
- On-chain settlement recording
- Web3 wallet connection (MetaMask)
- Transparent result finalization

## Environment Configuration

Create `.env.local` in the project root:

```env
# Database
DB_PASSWORD=your_mariadb_password

# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080

# Google OAuth (optional)
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
```

For backend configuration, see [backend/README.md](backend/README.md).

## API Documentation

The backend exposes RESTful APIs at `http://localhost:8080/api`:

- `POST /api/vote` - Submit a vote
- `POST /api/bet` - Place a bet
- `GET /api/questions` - List all questions
- `GET /api/questions/{id}` - Get question details
- `GET /api/tickets/{memberId}` - Check remaining tickets

Full API documentation: [backend/README.md](backend/README.md)

## Database Schema

The application uses a unified MariaDB database with the following core tables:

- `members` - User accounts and profiles
- `questions` - Prediction questions
- `activities` - Vote and bet records
- `daily_tickets` - 5-lock ticket management

Schema initialization is handled automatically via `init-db.sql`.

## Development Workflow

1. **Start infrastructure**: Run `./run-local.sh` to start MariaDB, Redis, and backend
2. **Start frontend**: Run `npm run dev` in a separate terminal
3. **Make changes**: Edit frontend code in `src/` or backend code in `backend/src/`
4. **Test**: Frontend hot-reloads automatically; backend requires `./gradlew bootRun` restart

## Testing

```bash
# Frontend tests
npm run test

# Backend tests
cd backend && ./gradlew test
```

## Deployment

### Frontend (Vercel)

```bash
# Deploy to Vercel
npm run build
vercel deploy
```

### Backend (Docker)

```bash
# Build backend Docker image
cd backend
docker build -t predata-backend .

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://host:3306/predata \
  -e SPRING_DATASOURCE_PASSWORD=password \
  predata-backend
```

## Tech Stack

### Frontend
- Next.js 15 (App Router)
- React 19
- TypeScript
- TailwindCSS
- Radix UI components

### Backend
- Spring Boot 3.2.1
- Kotlin 1.9.22
- Spring Data JPA
- Spring Security + OAuth2
- Web3j (blockchain)

### Infrastructure
- MariaDB 10.11 (primary database)
- Redis 7 (caching)
- Docker & Docker Compose

## Additional Resources

- [Backend Documentation](backend/README.md) - Detailed Spring Boot API docs
- [Betting Suspension System](BETTING_SUSPENSION_SYSTEM.md) - Real-time betting logic
- [Ultra Realtime System](ULTRA_REALTIME_SYSTEM.md) - Performance optimization
- [Web3 Integration](WEB3_INTEGRATION_COMPLETE.md) - Blockchain connection guide

## License

MIT License

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
