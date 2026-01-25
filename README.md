# P2P LAN File Transfer

Ứng dụng truyền file peer-to-peer với giao diện TUI (Terminal User Interface) để chia sẻ file giữa các thiết bị trong cùng mạng LAN.

![Java](https://img.shields.io/badge/Java-21+-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

## Tính năng

- 🔍 **Tự động phát hiện**: Phát hiện các peer trên mạng LAN qua UDP broadcast
- 📁 **Trình duyệt file**: Duyệt và chọn file trực tiếp trong TUI
- 🖥️ **Giao diện TUI**: Giao diện terminal đẹp mắt với Lanterna
- 🔄 **Cập nhật realtime**: Danh sách peer tự động refresh mỗi 3 giây
- 📤 **Truyền trực tiếp**: P2P file transfer qua kết nối TCP
- ✅ **Xác minh MD5**: Kiểm tra tính toàn vẹn file bằng MD5 checksum

## Yêu cầu

- Java 21 hoặc cao hơn
- Maven 3.6+
- Các thiết bị phải cùng mạng LAN

## Cài đặt & Chạy

### Build

```bash
mvn clean package
```

### Chạy ứng dụng

```bash
# Cách 1: Chạy với Maven
mvn exec:java

# Cách 2: Chạy JAR
java -jar target/FileTransferTUI-javaSocket-1.0-SNAPSHOT.jar
```

## Hướng dẫn sử dụng

### Giao diện chính

```
╔════════════════════════════════════════════╗
║       P2P LAN File Transfer System         ║
╚════════════════════════════════════════════╝
┌─Local Info─────────────────────────────────┐
│ You: dau @ 192.168.1.100      Time: 12:30  │
└────────────────────────────────────────────┘

Discovered Peers: 2
╔═Online Peers═══════════════════════════════╗
║  1. john            @ 192.168.1.101 [Send] ║
║  2. mary            @ 192.168.1.102 [Send] ║
╚════════════════════════════════════════════╝

[↻ Refresh] [📁 Send File] [✕ Exit]

┌─Status─────────────────────────────────────┐
│ Ready - 2 peer(s) online                   │
└────────────────────────────────────────────┘
[Tab] Navigate  [Enter] Select  [Esc] Exit
```

### Phím tắt

| Phím    | Chức năng                     |
| ------- | ----------------------------- |
| `Tab`   | Di chuyển giữa các thành phần |
| `Enter` | Chọn/Xác nhận                 |
| `Esc`   | Thoát/Hủy                     |

### Gửi file

1. Đợi các peer xuất hiện trong danh sách
2. Nhấn `Send →` bên cạnh peer, hoặc nhấn `📁 Send File`
3. Duyệt đến file muốn gửi
4. Chọn file để bắt đầu gửi

### Nhận file

File được nhận tự động và lưu vào thư mục `./downloads/`

## Cấu trúc project

```
src/main/java/FileTransfer/
├── app/
│   └── Main.java                 # Entry point
├── core/
│   ├── discovery/
│   │   ├── UdpBroadcaster.java   # Broadcast UDP
│   │   └── UdpListener.java      # Lắng nghe UDP
│   ├── network/
│   │   ├── MessageParser.java    # Parse message
│   │   ├── PeerConnection.java   # Kết nối TCP
│   │   └── TcpServer.java        # TCP server
│   ├── peer/
│   │   ├── Peer.java             # Model peer
│   │   └── PeerManager.java      # Quản lý peers
│   ├── protocol/
│   │   ├── MessageType.java      # Enum message types
│   │   └── Protocol.java         # Constants & builders
│   └── transfer/
│       ├── FileReceiver.java     # Nhận file
│       └── FileSender.java       # Gửi file
└── ui/
    ├── FileBrowserDialog.java    # Dialog chọn file
    ├── PeerSelectionDialog.java  # Dialog chọn peer
    └── TUIManager.java           # Quản lý TUI
```

## Network Protocol

### Ports

| Port  | Protocol | Mục đích       |
| ----- | -------- | -------------- |
| 50000 | UDP      | Peer discovery |
| 50001 | TCP      | File transfer  |

### Message Types

| Type           | Mô tả                  |
| -------------- | ---------------------- |
| `DISCOVER`     | Broadcast sự hiện diện |
| `HELLO`        | Handshake kết nối TCP  |
| `SEND_REQUEST` | Yêu cầu gửi file       |
| `SEND_ACCEPT`  | Chấp nhận nhận file    |
| `SEND_REJECT`  | Từ chối nhận file      |
| `START_SEND`   | Bắt đầu truyền data    |
| `DONE`         | Hoàn tất transfer      |
| `CANCEL`       | Hủy transfer           |
| `ERROR`        | Có lỗi xảy ra          |

### Message Format

```
MESSAGE_TYPE|field1|field2|...\n
```

Ví dụ:

```
DISCOVER|192.168.1.100|dau|50001
SEND_REQUEST|document.pdf|1048576
```

## Cấu hình mặc định

```java
UDP_PORT = 50000          // Port discovery
TCP_PORT = 50001          // Port transfer
DISCOVER_INTERVAL = 3000  // Broadcast interval (ms)
DISCOVER_TIMEOUT = 10000  // Peer timeout (ms)
```

## Xử lý sự cố

### Không tìm thấy peer

- Đảm bảo các thiết bị cùng subnet mạng
- Kiểm tra firewall cho UDP port 50000 và TCP port 50001
- Đảm bảo ứng dụng đang chạy trên thiết bị khác

### Kết nối thất bại

- Kiểm tra peer còn online không
- Verify TCP port 50001 không bị block
- Thử refresh lại danh sách peer

### Không nhận được file

- Kiểm tra thư mục `./downloads/` tồn tại và có quyền ghi
- Kiểm tra dung lượng ổ đĩa

## Dependencies

- [Lanterna 3.1.2](https://github.com/mabe02/lanterna) - Terminal UI library

## License

MIT License

---

Built with ❤️ using Java Sockets & Lanterna TUI
