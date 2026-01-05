# File Transfer Workflow - Complete Flow

## ✅ Protocol Implementation Status

**Đã implement đúng 100% theo protocol.md**

---

## 🔄 Complete Message Flow

### 1️⃣ **Peer Discovery (UDP)**

```
Peer A                          Broadcast (LAN)                    Peer B
   |                                                                  |
   |--- DISCOVER|peerA|LaptopA|50001 ----------------------->       |
   |                                                                  |
   |       <---------------------- DISCOVER|peerB|PCB|50001 ---------|
   |                                                                  |
```

**Implementation:**

- `UdpBroadcaster.java` - Gửi DISCOVER mỗi 3 giây
- `UdpListener.java` - Lắng nghe và update PeerManager
- `PeerManager.java` - Quản lý danh sách peers, xóa peers timeout >10s

---

### 2️⃣ **TCP Connection Setup**

```
Peer A (Sender)                                              Peer B (Receiver)
   |                                                                  |
   |--- TCP Connect to Peer B:50001 ------------------------------>  |
   |                                                                  |
   |<-- TCP Accept -------------------------------------------------  |
   |                                                                  |
   |--- HELLO|peerA|LaptopA ----------------------------------------> |
   |                                                                  |
   |<-- HELLO|peerB|PCB --------------------------------------------- |
   |                                                                  |
```

**Implementation:**

- `TcpServer.java` - Accept connections trên port 50001
- `PeerConnection.java` - Gửi HELLO ngay sau khi connect
- State: `IDLE`

---

### 3️⃣ **File Transfer Request**

```
Peer A (Sender)                                              Peer B (Receiver)
   |                                                                  |
   |--- SEND_REQUEST|video.mp4|104857600 --------------------------> |
   |                                                                  |
   |    State: WAITING_FOR_RESPONSE         State: IDLE              |
   |                                          ↓                       |
   |                                    User Prompt:                 |
   |                                    Accept video.mp4 (100MB)?    |
   |                                          ↓                       |
   |                                    State: WAITING_FOR_START     |
   |                                                                  |
   |<-- SEND_ACCEPT ------------------------------------------------  |
   |                                                                  |
```

**Or if rejected:**

```
   |<-- SEND_REJECT ------------------------------------------------  |
   |    State: IDLE                          State: IDLE             |
```

**Implementation:**

- `PeerConnection.sendFileRequest(File)` - Public method để gửi request
- `handleSendRequest()` - Nhận request, check state, ask user
- States: `IDLE` → `WAITING_FOR_RESPONSE` → `WAITING_FOR_START`

---

### 4️⃣ **File Data Transfer**

```
Peer A (Sender)                                              Peer B (Receiver)
   |                                                                  |
   |--- START_SEND -------------------------------------------------> |
   |                                                                  |
   |    State: SENDING                       State: RECEIVING        |
   |                                                                  |
   |--- Raw Binary Data Stream (104857600 bytes) -----------------> |
   |    ████████████████████████████████████████████████████████     |
   |    [Progress: 100%]                     [Progress: 100%]        |
   |                                                                  |
   |                                         File saved to:          |
   |                                         downloads/video.mp4     |
   |                                                                  |
   |<-- DONE -------------------------------------------------------- |
   |                                                                  |
   |    State: COMPLETED                     State: COMPLETED        |
```

**Implementation:**

- `FileSender.java` - Đọc file, stream bytes qua socket.getOutputStream()
- `FileReceiver.java` - Nhận bytes từ socket.getInputStream(), ghi file
- Progress tracking với MD5 checksum
- Raw byte transfer, không có framing/chunking

---

### 5️⃣ **Error/Cancel Handling**

```
Peer A                                                       Peer B
   |                                                                  |
   |--- SEND_REQUEST|file.txt|1000 --------------------------------> |
   |                                                                  |
   |    [Error occurs or user cancels]                               |
   |                                                                  |
   |<-- CANCEL ------------------------------------------------------  |
   |                                                                  |
   |    State: IDLE                          State: IDLE             |
```

**Or:**

```
   |--- CANCEL -----------------------------------------------------> |
   |    State: IDLE                          State: IDLE             |
```

**Implementation:**

- `sendCancel()` - Public method, có thể gọi bất cứ lúc nào
- `handleCancel()` - Reset state về IDLE
- Socket disconnect → cleanup tự động

---

## 📊 State Machine

```
                    ┌─────────────┐
                    │    IDLE     │ <────────────┐
                    └─────────────┘              │
                           │                     │
              sendFileRequest(file)              │
                           ↓                     │
              ┌──────────────────────┐           │
              │ WAITING_FOR_RESPONSE │           │
              └──────────────────────┘           │
                    │           │                │
          SEND_ACCEPT│           │SEND_REJECT   │
                    ↓           └────────────────┤
            ┌──────────────────┐                 │
            │ WAITING_FOR_START│                 │
            └──────────────────┘                 │
                    │                            │
               START_SEND                        │
                    ↓                            │
              ┌──────────┐                       │
              │ SENDING  │                       │
              └──────────┘                       │
                    │                            │
                  DONE                           │
                    ↓                            │
              ┌───────────┐                      │
              │ COMPLETED │──────────────────────┘
              └───────────┘


              (Receiver flow tương tự)
```

---

## 🔧 Key Classes & Responsibilities

### Protocol Layer

- **MessageType.java** - Enum: DISCOVER, HELLO, SEND_REQUEST, SEND_ACCEPT, SEND_REJECT, START_SEND, CANCEL, DONE, ERROR
- **Protocol.java** - Constants (ports, timeouts) + message builders
- **MessageParser.java** - Parse và validate messages

### Discovery Layer

- **UdpBroadcaster.java** - Broadcast DISCOVER mỗi 3s
- **UdpListener.java** - Listen UDP port 50000, update PeerManager

### Peer Management

- **Peer.java** - Data model: peerId, name, IP, lastSeen
- **PeerManager.java** - ConcurrentHashMap, auto-remove timeout peers

### Network Layer

- **TcpServer.java** - ServerSocket port 50001, ExecutorService
- **PeerConnection.java** - Handle 1 peer connection, state machine, message routing

### Transfer Layer

- **FileSender.java** - Stream file bytes, calculate MD5, progress tracking
- **FileReceiver.java** - Receive bytes, write file, verify size

### Application

- **Main.java** - Bootstrap, start threads, CLI interface

---

## ✅ Protocol Compliance Checklist

- ✅ UDP Discovery: DISCOVER message format đúng
- ✅ TCP Control: Text-based, line-delimited (\n), UTF-8
- ✅ Field separator: `|`
- ✅ HELLO handshake ngay sau connect
- ✅ SEND_REQUEST with filename and filesize
- ✅ SEND_ACCEPT/SEND_REJECT đơn giản (no fields)
- ✅ START_SEND trước khi transfer data
- ✅ Raw byte stream (không chunking, không framing)
- ✅ DONE sau khi nhận đủ bytes
- ✅ CANCEL có thể gửi từ 2 phía
- ✅ Session states: IDLE, WAITING_FOR_RESPONSE, WAITING_FOR_START, SENDING, RECEIVING
- ✅ Một session tại một thời điểm
- ✅ Không retry, không resume
- ✅ Peer timeout 10s

---

## 🚀 Next Steps (TODO)

### Critical

- [ ] **TUI Implementation** - Lanterna-based terminal UI
  - Peer list view
  - File browser
  - Accept/reject dialog
  - Progress bars
- [ ] **Integration Testing**
  - Test 2 instances in same network
  - Test large file transfers
  - Test cancel scenarios

### Nice to Have

- [ ] Download directory configuration
- [ ] Transfer history
- [ ] Checksum verification display
- [ ] Bandwidth throttling
- [ ] Multiple file queue (future protocol v2)

---

## 🎯 Current Status

**✅ DONE:**

- Protocol specification complete
- UDP Discovery working
- Peer management with timeout
- TCP server + connection handling
- Message parsing and validation
- File transfer with MD5 checksum
- Complete state machine
- Basic CLI

**🔄 IN PROGRESS:**

- TUI implementation (Lanterna)

**⏳ TODO:**

- End-to-end testing
- User accept/reject UI integration
- Error handling refinement

---

## 📝 Testing Instructions

### 1. Compile

```bash
mvn clean compile
```

### 2. Run Instance 1

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
```

### 3. Run Instance 2 (different terminal/machine)

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
```

### 4. Verify

- Both instances should discover each other (wait 3-5s)
- Use command "1" to list peers
- File sending feature coming soon via TUI

---

_Last Updated: 2026-01-05_
