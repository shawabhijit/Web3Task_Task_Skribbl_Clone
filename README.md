# 🎨 DrawVerse — Skribbl.io Clone

> A full-stack real-time multiplayer drawing and guessing game inspired by Skribbl.io, built with **React + Vite** on the frontend and **Spring Boot + WebSocket** on the backend.

---

## 🚀 Live Deployment

| Service | URL |
|---------|-----|
| 🌐 Frontend | [https://web3-task-task-skribbl-clone-fwoxxqcc2.vercel.app/](https://web3-task-task-skribbl-clone-fwoxxqcc2.vercel.app/) |
| ⚙️ Backend | [https://web3task-task-skribbl-clone-trfv.onrender.com](https://web3task-task-skribbl-clone-trfv.onrender.com) |

> **Note:** The backend is hosted on Render's free tier — it may take **10–30 seconds to wake up** on first request after a period of inactivity.

---

## ✅ Features

### 🎮 Core Gameplay
- **Real-time multiplayer drawing & guessing** powered by WebSocket (STOMP over SockJS)
- **Turn-based rounds** — players take turns drawing while others guess
- **Live canvas sync** — every stroke is broadcast to all players in the room in real-time
- **Word selection** — drawer receives a word to draw per round

### 🏠 Room System
- **Create a room** with fully customizable settings
- **Public rooms** — browseable and joinable by anyone
- **Private rooms** — invite-only via shareable room code / link
- **Host controls** — only the room creator can start the game
- **Auto-generated 6-character room codes**

### ⚙️ Configurable Room Settings
- Max players (2–8)
- Number of rounds
- Draw time per round (seconds)
- Number of word choices per turn
- Number of hints
- Word mode (Normal / etc.)

### 🖊️ Drawing Tools
- **Freehand brush** with adjustable size
- **Eraser** tool
- **Undo** last stroke
- **Clear canvas** (broadcast to all viewers)
- **20-color palette** for drawing

### 💬 Chat & Guessing
- Live in-room chat visible to all players
- Guess submission during drawing phase
- Correct guesses are highlighted in green with player name
- Drawer is prevented from guessing their own word
- Players who already guessed correctly cannot re-guess

### 🏆 Scoring System
- **Time-based scoring** — faster correct guesses earn more points
- **Drawer points** — awarded incrementally as more players guess correctly
- Live scoreboard with player rankings displayed during the game
- Host and drawing-status indicators on player cards

### 🔗 Invite & Join Flow
- Shareable invite links (`/invite/:roomCode`)
- Join by room code directly
- Avatar carousel on the landing page

---

## 🛠️ Tech Stack

### Frontend
| Technology | Purpose |
|-----------|---------|
| React 19 + Vite 8 | UI framework & build tool |
| React Router DOM v7 | Client-side routing |
| Tailwind CSS v4 | Utility-first styling |
| @stomp/stompjs + SockJS | WebSocket client (STOMP protocol) |
| Fabric.js v7 | Canvas drawing engine |
| Lucide React | Icon library |

### Backend
| Technology | Purpose |
|-----------|---------|
| Spring Boot 4 | Application framework |
| Spring WebSocket (STOMP) | Real-time bidirectional communication |
| Spring Data JPA + Hibernate | ORM & database layer |
| PostgreSQL | Persistent data store |
| Lombok | Boilerplate reduction |
| Maven | Build & dependency management |
| Docker | Containerisation for deployment |

---

## 📁 Project Structure

```
Web3Task_Task_Skribbl_Clone/
└── DrawVerse/
    ├── frontend/                          # React + Vite application
    │   ├── index.html
    │   ├── vite.config.js
    │   ├── package.json
    │   └── src/
    │       ├── main.jsx                   # React entry point
    │       ├── App.jsx                    # Root component & route definitions
    │       ├── App.css
    │       ├── index.css
    │       └── components/
    │           ├── Button.jsx             # Reusable button component
    │           ├── CreateRoomPage.jsx     # Room creation form & settings
    │           ├── GamePage.jsx           # Main game view (canvas + chat + scoreboard)
    │           ├── InviteJoinPage.jsx     # Join via invite link / room code
    │           ├── Landing/
    │           │   ├── HeroSection.jsx    # Landing page hero
    │           │   └── AvatarCarousel.jsx # Avatar picker carousel
    │           ├── Lobbey/
    │           │   └── UseLobbey.jsx      # Lobby waiting room logic
    │           ├── api/
    │           │   └── roomApi.js         # REST API calls (create/join/leave room)
    │           ├── hooks/
    │           │   ├── useDrawingCanvas.js # Canvas draw logic & Fabric.js integration
    │           │   └── useGameSocket.js    # WebSocket connection & game event handling
    │           └── utils/
    │               └── PlayerIdetity.js   # Player ID generation & persistence
    │
    └── backend/                           # Spring Boot application
        ├── Dockerfile
        ├── pom.xml
        ├── mvnw / mvnw.cmd
        └── src/main/java/com/backend/
            ├── BackendApplication.java    # Spring Boot entry point
            ├── Config/
            │   ├── CorsConfig.java        # CORS configuration
            │   └── WebsocketConfig.java   # STOMP WebSocket broker config
            ├── Controller/
            │   ├── RoomController.java            # REST endpoints for room management
            │   ├── LobbyWebSocketController.java  # WS: player register, guess handling
            │   └── DrawingWebSocketController.java # WS: draw, undo, canvas clear
            ├── DTO/
            │   ├── PlayerDto.java
            │   └── RoomSettingsDto.java
            ├── Entity/
            │   ├── Player.java
            │   ├── Room.java
            │   ├── RoomSettings.java
            │   └── Enum/
            │       ├── PlayerStatus.java  # CONNECTED, DISCONNECTED
            │       ├── RoomStatus.java    # WAITING, IN_PROGRESS, FINISHED
            │       └── RoomType.java      # PUBLIC, PRIVATE
            ├── Exceptions/
            │   ├── GlobalExceptionsHandler.java
            │   └── RoomExceptions.java
            ├── Repository/
            │   ├── PlayerRepository.java
            │   └── RoomRepository.java
            ├── Service/
            │   ├── RoomService.java       # Core game logic: create, join, start, leave
            │   ├── GuessService.java      # Guess validation & point calculation
            │   └── WordService.java       # Word selection per round
            ├── Utils/
            │   ├── GameTimer.java         # Per-round countdown timer
            │   ├── LobbyEvent.java        # Lobby broadcast event helper
            │   ├── RoomCodeGenerator.java # Generates unique 6-char room codes
            │   ├── WebSocketEventListener.java # Disconnect/connect lifecycle events
            │   └── WordList.java          # Built-in word bank
            └── exchanges/
                ├── CreateRoomRequest.java
                ├── JoinRoomRequest.java
                ├── PlayerResponse.java
                ├── PublicRoomSummaryResponse.java
                └── RoomResponse.java
```

---

## ⚙️ Local Setup Instructions

### Prerequisites

| Tool | Version |
|------|---------|
| Node.js | ≥ 18.x |
| npm | ≥ 9.x |
| Java JDK | 21 |
| Maven | 3.9+ |
| PostgreSQL | 14+ |

---

### 1. Clone the Repository

```bash
git clone https://github.com/shawabhijit/Web3Task_Task_Skribbl_Clone.git
cd Web3Task_Task_Skribbl_Clone/DrawVerse
```

---

### 2. Backend Setup

#### 2a. Configure the Database

Create a PostgreSQL database:

```sql
CREATE DATABASE skribbl_clone_db;
```

#### 2b. Update `application.properties`

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/skribbl_clone_db
spring.datasource.username=YOUR_PG_USERNAME
spring.datasource.password=YOUR_PG_PASSWORD

# Allow your frontend dev server origin
app.cors.allowed-origins=http://localhost:5173
```

#### 2c. Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start on **http://localhost:8080**.

---

### 3. Frontend Setup

#### 3a. Install Dependencies

```bash
cd ../frontend
npm install
```

#### 3b. Configure the Backend URL

The frontend connects to the backend via the `roomApi.js` and WebSocket hooks. If running locally, ensure the API base URL in `src/components/api/roomApi.js` points to `http://localhost:8080`.

#### 3c. Start the Dev Server

```bash
npm run dev
```

The frontend will start on **http://localhost:5173**.

---

### 4. (Optional) Run Backend with Docker

```bash
cd backend
docker build -t drawverse-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/skribbl_clone_db \
  -e SPRING_DATASOURCE_USERNAME=your_user \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  drawverse-backend
```

---

## 🌐 REST API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/rooms` | Create a new room |
| `POST` | `/api/v1/rooms/join` | Join a room by code |
| `GET` | `/api/v1/rooms/{roomCode}` | Get room details |
| `GET` | `/api/v1/rooms/public` | List all public rooms |
| `DELETE` | `/api/v1/rooms/{roomCode}/leave` | Leave a room |
| `POST` | `/api/v1/rooms/{roomCode}/start` | Host starts the game |

## 🔌 WebSocket Topics (STOMP)

| Destination | Direction | Description |
|-------------|-----------|-------------|
| `/app/player.register` | Client → Server | Register player session |
| `/app/room.{code}.draw` | Client → Server | Send drawing stroke |
| `/app/room.{code}.canvas_clear` | Client → Server | Clear canvas |
| `/app/room.{code}.draw_undo` | Client → Server | Undo last stroke |
| `/app/room.{code}.guess` | Client → Server | Submit a word guess |
| `/topic/room.{code}` | Server → Client | Broadcast draw data |
| `/topic/room.{code}.guess_result` | Server → Client | Correct/incorrect guess result |
| `/topic/room.{code}.error` | Server → Client | Error events |

---

## 📦 Build for Production

### Frontend

```bash
cd frontend
npm run build
# Output in /dist — deploy to Vercel, Netlify, or any static host
```

### Backend

```bash
cd backend
./mvnw clean package -DskipTests
# Output JAR: target/backend-0.0.1-SNAPSHOT.jar
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## 👤 Author

**Abhijit Shaw**
GitHub: [@shawabhijit](https://github.com/shawabhijit)
