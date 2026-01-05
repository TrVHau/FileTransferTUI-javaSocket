# TUI Guide - LAN File Transfer

## 🎨 Terminal User Interface

Ứng dụng đã được trang bị **Terminal UI** đầy đủ sử dụng thư viện Lanterna.

---

## 🚀 Cách chạy

### 1. Chạy với TUI (mặc định)

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
```

### 2. Chạy với CLI (text-only mode)

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main" -Dexec.args="--cli"
```

---

## 🖥️ TUI Features

### Màn hình chính

```
=================================
=== P2P File Transfer ===
=================================

Online Peers:
┌──────────────────────────────────────────────┐
│ [1] Alice (192.168.1.100) - 192.168.1.100   │ [Select]
│ [2] Bob (192.168.1.101) - 192.168.1.101     │ [Select]
│ [3] Charlie (192.168.1.102) - 192.168.1.102 │ [Select]
└──────────────────────────────────────────────┘

[Refresh]   [Send File]   [Exit]

Status: Peers: 3
```

### Chức năng:

#### 1️⃣ **Refresh** - Cập nhật danh sách peers

- Auto-refresh mỗi 5 giây
- Xóa peers timeout (>10s không phản hồi)
- Hiển thị số lượng peers online

#### 2️⃣ **Send File** - Gửi file đến peer

**Workflow:**

1. Click "Send File" hoặc "Select" trên peer
2. Chọn peer từ dialog (nếu click Send File)
3. File Browser hiện ra
4. Navigate thư mục: Click vào `[DIR] folder_name`
5. Quay lại: Click "Parent Dir"
6. Chọn file: Click vào file name
7. File được gửi tự động

**File Browser:**

```
┌─────── Select File ──────────┐
│ Path: /home/user/Documents   │
│                              │
│ ┌─── Files ────────────────┐ │
│ │ [DIR] Projects           │ │
│ │ [DIR] Downloads          │ │
│ │ document.pdf (2.5 MB)    │ │
│ │ image.png (512 KB)       │ │
│ │ video.mp4 (100 MB)       │ │
│ └──────────────────────────┘ │
│                              │
│ [Parent Dir]   [Cancel]      │
└──────────────────────────────┘
```

#### 3️⃣ **Exit** - Đóng ứng dụng

- Graceful shutdown
- Đóng tất cả connections
- Dừng UDP broadcast/listen

---

## ⌨️ Keyboard Shortcuts

| Key          | Action                      |
| ------------ | --------------------------- |
| `Tab`        | Di chuyển giữa các controls |
| `Enter`      | Select/Click button         |
| `Esc`        | Cancel/Close window         |
| `Arrow Keys` | Navigate lists              |
| `Space`      | Select item in radio list   |

---

## 📊 Peer Discovery

- **Auto-discovery**: Peers tự động xuất hiện trong list
- **Timeout**: Peers mất >10s sẽ bị xóa khỏi list
- **Refresh**: Auto mỗi 5s, hoặc click "Refresh"

---

## 📤 File Transfer Flow (TUI)

```
┌─────────────┐
│  Main TUI   │
└──────┬──────┘
       │
       │ [Click Send File]
       ↓
┌─────────────────┐
│ Select Peer     │
│ ○ Alice         │
│ ○ Bob           │
│ ○ Charlie       │
│                 │
│ [OK]  [Cancel]  │
└──────┬──────────┘
       │ [Select peer + OK]
       ↓
┌─────────────────────┐
│ File Browser        │
│ Path: /home/user    │
│                     │
│ [DIR] Documents     │ ← Click to navigate
│ [DIR] Downloads     │
│ file.txt (1 KB)     │ ← Click to send
│ video.mp4 (100 MB)  │
│                     │
│ [Parent] [Cancel]   │
└──────┬──────────────┘
       │ [Select file]
       ↓
   Connecting...
       ↓
   HELLO handshake
       ↓
   SEND_REQUEST
       ↓
   [Peer accepts]
       ↓
   START_SEND
       ↓
   Transferring...
   ████████████ 100%
       ↓
   DONE ✓
```

---

## 🔔 Receiving Files (TODO - Next Phase)

**Khi nhận file request:**

```
┌──────────────────────────────┐
│ Incoming File Transfer       │
│                              │
│ From: Alice (192.168.1.100)  │
│ File: document.pdf           │
│ Size: 2.5 MB                 │
│                              │
│ Accept this file?            │
│                              │
│ [Accept]   [Reject]          │
└──────────────────────────────┘
```

**During transfer:**

```
┌──────────────────────────────┐
│ Receiving File               │
│                              │
│ File: document.pdf           │
│                              │
│ Progress:                    │
│ ████████████░░░░ 75%         │
│ 1.9 MB / 2.5 MB              │
│                              │
│ [Cancel]                     │
└──────────────────────────────┘
```

---

## 🎯 TUI Components Created

### 1. **TUIManager.java**

- Main UI controller
- Window management
- Event handling
- Peer list refresh
- Status updates

### 2. **PeerSelectionDialog.java**

- Dialog để chọn peer
- RadioBoxList với danh sách peers
- OK/Cancel buttons

### 3. **FileBrowserDialog.java**

- File system navigation
- Directory browsing
- File selection
- Parent directory navigation

---

## 🛠️ Customization

### Thay đổi màu sắc:

Sửa trong `TUIManager.java`:

```java
titleLabel.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
peerLabel.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
statusLabel.setForegroundColor(TextColor.ANSI.GREEN);
```

### Thay đổi kích thước:

```java
peerListPanel.setPreferredSize(new TerminalSize(60, 10));
fileListBox = new ActionListBox(new TerminalSize(60, 15));
```

### Thay đổi refresh interval:

```java
Thread.sleep(5000); // 5 seconds → change to desired value
```

---

## 🐛 Troubleshooting

### TUI không hiển thị:

```bash
# Kiểm tra terminal support
echo $TERM

# Nếu không support, dùng CLI mode:
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main" -Dexec.args="--cli"
```

### Không thấy peers:

1. Đợi 3-5 giây (discovery interval)
2. Click "Refresh"
3. Kiểm tra firewall (ports 50000, 50001)
4. Kiểm tra cùng network/subnet

### File transfer fails:

1. Kiểm tra file permissions
2. Kiểm tra disk space
3. Xem log trong terminal
4. Kiểm tra network connectivity

---

## 📝 Next Steps

### Phase 1 (Current) ✅

- ✅ TUI main window
- ✅ Peer list display
- ✅ File browser
- ✅ Send file integration

### Phase 2 (TODO)

- [ ] Receive file dialog (accept/reject)
- [ ] Progress bars for transfers
- [ ] Transfer history view
- [ ] Error dialogs with details
- [ ] Checksum verification display

### Phase 3 (Future)

- [ ] Multiple file queue
- [ ] Drag-and-drop support (if terminal supports)
- [ ] Themes/color schemes
- [ ] Configuration dialog
- [ ] Statistics view

---

## 🎬 Demo Usage

### Test với 2 instances:

**Terminal 1:**

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
# Đợi TUI load
# Thấy peers xuất hiện
# Click "Send File"
# Chọn file
# Wait for transfer
```

**Terminal 2 (cùng network):**

```bash
mvn exec:java -Dexec.mainClass="FileTransfer.app.Main"
# Đợi TUI load
# Thấy peer từ Terminal 1
# Tự động accept file (TODO: add UI prompt)
# File được save vào downloads/
```

---

_Happy File Transferring! 🚀_
