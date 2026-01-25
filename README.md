# FileTransferTUI - P2P LAN File Transfer Application

A peer-to-peer file transfer application with Terminal User Interface (TUI) for sharing files over Local Area Network using Java sockets.

## Features

- 🌐 **Peer-to-Peer Discovery**: Automatic peer discovery via UDP broadcast
- 📁 **File Transfer**: Direct file transfer between peers using TCP
- 🖥️ **Terminal UI**: Clean and intuitive text-based user interface powered by Lanterna
- 🔒 **Protocol-Based**: Custom text-based protocol for reliable communication
- ✅ **MD5 Checksum**: File integrity verification using MD5 checksums
- 🚀 **Zero Configuration**: No server required, fully decentralized

## Requirements

- Java 17 or higher
- Maven 3.6+
- Network connectivity on the same LAN

## Installation

1. Clone the repository:

```bash
git clone https://github.com/TrVHau/FileTransferTUI-javaSocket.git
cd FileTransferTUI-javaSocket
```

2. Build the project:

```bash
mvn clean compile
```

## Usage

### Running the Application

Start the application using Maven:

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
```

Or create a JAR file and run it:

```bash
mvn clean package
java -jar target/FileTransferTUI-javaSocket-1.0-SNAPSHOT.jar
```

### Using the TUI

1. **Main Screen**: Shows list of discovered peers on your LAN

   - Peers are automatically discovered and listed
   - Your own device is filtered out from the list

2. **Send File**:

   - Press `S` key or click "Send File" button
   - Select a peer from the list
   - Browse and select a file to send
   - Wait for recipient to accept

3. **Exit**:
   - Press `Q` key or click "Quit" button
   - Application will shutdown cleanly

### Keyboard Shortcuts

- `S` - Send File
- `Q` - Quit
- `R` - Refresh peer list
- `Arrow Keys` - Navigate UI elements
- `Enter` - Select/Confirm
- `Esc` - Cancel/Go back

## Protocol

The application uses a custom text-based protocol over TCP/UDP:

### UDP Discovery (Port 50000)

- `DISCOVER|<peerId>|<peerName>|<tcpPort>` - Broadcast peer presence

### TCP Control Messages (Port 50001)

- `HELLO|<peerId>|<peerName>` - Handshake
- `SEND_REQUEST|<filename>|<filesize>` - Request to send file
- `SEND_ACCEPT` - Accept file transfer
- `SEND_REJECT` - Reject file transfer
- `START_SEND` - Begin file transmission
- `DONE` - Transfer completed
- `CANCEL` - Cancel transfer
- `ERROR|<message>` - Error occurred

See [protocol.md](protocol.md) for detailed protocol specification.

## Project Structure

```
src/main/java/FileTransfer/
├── app/
│   └── Main.java                 # Application entry point
├── core/
│   ├── discovery/
│   │   ├── UdpBroadcaster.java  # UDP broadcast for peer discovery
│   │   └── UdpListener.java     # UDP listener for discovering peers
│   ├── network/
│   │   ├── MessageParser.java    # Protocol message parser
│   │   ├── PeerConnection.java   # Handles peer-to-peer connection
│   │   └── TcpServer.java        # TCP server for incoming connections
│   ├── peer/
│   │   ├── Peer.java             # Peer data model
│   │   └── PeerManager.java      # Manages discovered peers
│   ├── protocol/
│   │   ├── MessageType.java      # Message type enumeration
│   │   └── Protocol.java         # Protocol constants and builders
│   └── transfer/
│       ├── FileReceiver.java     # Handles file reception
│       └── FileSender.java       # Handles file transmission
└── ui/
    ├── FileBrowserDialog.java    # File selection dialog
    ├── PeerSelectionDialog.java  # Peer selection dialog
    └── TUIManager.java           # Main TUI window manager
```

## Network Configuration

### Ports Used

- **UDP 50000**: Peer discovery broadcasts
- **TCP 50001**: File transfer control and data

### Firewall Configuration

If you're having connection issues, ensure these ports are open:

**Linux (iptables):**

```bash
sudo iptables -A INPUT -p udp --dport 50000 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 50001 -j ACCEPT
```

**Linux (firewalld):**

```bash
sudo firewall-cmd --add-port=50000/udp --permanent
sudo firewall-cmd --add-port=50001/tcp --permanent
sudo firewall-cmd --reload
```

**macOS:**

```bash
# Allow in System Preferences > Security & Privacy > Firewall
# Or disable firewall temporarily for testing
```

## Troubleshooting

### No Peers Discovered

- Check if all devices are on the same network/subnet
- Verify firewall settings (UDP port 50000)
- Ensure application is running on both devices
- Check if broadcast packets are allowed on your network

### Connection Failed

- Verify TCP port 50001 is not blocked
- Check if both devices can ping each other
- Ensure no other application is using port 50001

### File Transfer Fails

- Check available disk space
- Verify file permissions
- Check network stability
- Look for firewall interference

### TUI Display Issues

- Ensure terminal supports ANSI escape codes
- Try resizing terminal window
- Use a terminal emulator with better compatibility (e.g., GNOME Terminal, iTerm2)

## Development

### Building from Source

```bash
# Compile
mvn clean compile

# Run tests (if any)
mvn test

# Create JAR
mvn clean package

# Run with debugging
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
```

### Dependencies

- **Lanterna 3.1.1**: Terminal UI framework
- **Java Socket API**: Network communication
- **Maven**: Build and dependency management

## Architecture

The application follows a modular architecture:

1. **Discovery Layer**: UDP-based peer discovery with automatic timeout
2. **Network Layer**: TCP-based reliable communication
3. **Transfer Layer**: Chunked file transfer with MD5 verification
4. **UI Layer**: Event-driven terminal interface

### State Machine

Each peer connection follows a state machine:

- IDLE → WAITING_FOR_RESPONSE → WAITING_FOR_START → SENDING/RECEIVING → COMPLETED

## Security Considerations

⚠️ **Important**: This application is designed for trusted local networks only.

- No encryption is implemented
- No authentication mechanism
- Files are transferred in plain text
- Suitable for home/office LANs, not public networks

For production use, consider adding:

- TLS/SSL encryption
- User authentication
- File transfer authorization
- Access control lists

## Performance

- **Transfer Speed**: Limited by network bandwidth
- **Buffer Size**: 8KB chunks for optimal performance
- **Discovery Interval**: 3 seconds between broadcasts
- **Peer Timeout**: 10 seconds of inactivity

## Known Limitations

- Single file transfer at a time (no concurrent transfers)
- No transfer resume/retry capability
- No compression
- Limited to LAN (no WAN/Internet support)
- IPv4 only

## Future Enhancements

- [ ] Multiple simultaneous transfers
- [ ] Transfer progress bars in TUI
- [ ] File transfer history
- [ ] Transfer resume support
- [ ] File compression
- [ ] IPv6 support
- [ ] Encryption (TLS/SSL)
- [ ] User authentication

## License

no license

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Authors

Trần Văn Dậu

## Acknowledgments

- [Lanterna](https://github.com/mabe02/lanterna) - Terminal UI library
- Protocol design inspired by common P2P file sharing applications
