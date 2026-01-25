# P2P LAN File Share – Protocol Specification

## 1. Phạm vi

- Ứng dụng chia sẻ file **P2P trong LAN** (LAN vật lý hoặc LAN ảo như Tailscale, Radmin)
- **Không có server trung tâm**
- Mỗi thời điểm **chỉ truyền 1 file**
- Hai phía **phải mở ứng dụng**
- Hai phía **đều phải xác nhận** trước khi truyền file

---

## 2. Kiến trúc mạng

- **UDP**: dùng cho peer discovery (tự tìm các máy trong LAN)
- **TCP**: dùng cho control protocol và truyền dữ liệu file
- Mỗi instance của ứng dụng vừa đóng vai trò **client** vừa là **server**

---

## 3. Cổng mặc định

| Mục đích | Giao thức | Cổng |
|--------|----------|------|
| Peer discovery | UDP | 50000 |
| Control + File transfer | TCP | 50001 |

- Các cổng có thể cho phép cấu hình lại trong tương lai
- Mặc định giả định các peer nằm trong cùng subnet

---

## 4. Discovery Protocol (UDP)

### 4.1 Mục đích

- Tự động phát hiện các peer đang online trong LAN
- Không yêu cầu server trung gian
- Không đảm bảo delivery (best-effort)

### 4.2 Discovery Message Format

- Encoding: UTF-8
- Dạng text, một gói tin một message

```
DISCOVER|<peer_id>|<device_name>|<tcp_port>
```

Ví dụ:

```
DISCOVER|a8f3c2|Laptop-Dau|50001
```

### 4.3 Quy tắc hoạt động

- Mỗi peer broadcast message mỗi 2–3 giây
- Khi nhận message:
  - Lấy IP nguồn của gói UDP
  - Lưu thông tin peer vào danh sách online
  - Cập nhật thời điểm `lastSeen`
- Nếu không nhận message từ peer trong 10 giây → peer bị coi là offline

---

## 5. Control Protocol (TCP)

### 5.1 Nguyên tắc chung

- Text-based protocol
- Mỗi message là **một dòng**, kết thúc bằng `\n`
- Các trường phân tách bằng ký tự `|`
- Encoding: UTF-8

---

### 5.2 Message Definitions

#### HELLO

Gửi ngay sau khi TCP connection được thiết lập.

```
HELLO|<peer_id>|<device_name>
```

---

#### SEND_REQUEST

Bên gửi yêu cầu gửi file.

```
SEND_REQUEST|<filename>|<filesize>
```

Ví dụ:

```
SEND_REQUEST|video.mp4|104857600
```

---

#### SEND_ACCEPT

Bên nhận đồng ý nhận file.

```
SEND_ACCEPT
```

---

#### SEND_REJECT

Bên nhận từ chối nhận file.

```
SEND_REJECT
```

---

#### START_SEND

Bên gửi xác nhận bắt đầu truyền dữ liệu.

```
START_SEND
```

---

#### CANCEL

Huỷ phiên truyền (có thể gửi từ bất kỳ bên nào).

```
CANCEL
```

---

#### DONE

Bên nhận thông báo đã nhận file thành công.

```
DONE
```

---

## 6. File Transfer

- Dữ liệu file được gửi **ngay sau** message `START_SEND`
- Truyền dưới dạng **raw byte stream** qua cùng TCP connection
- Không có framing đặc biệt
- Bên nhận đọc đúng `filesize` byte đã khai báo
- Sau khi nhận đủ dữ liệu:
  - Ghi file ra disk
  - Gửi message `DONE`

---

## 7. Session State

Mỗi peer chỉ xử lý **một session tại một thời điểm**.

Các trạng thái hợp lệ:

- `IDLE`
- `WAITING_FOR_RESPONSE`
- `WAITING_FOR_START`
- `SENDING`
- `RECEIVING`

Quy tắc:

- Khi đang `SENDING` hoặc `RECEIVING` → từ chối mọi yêu cầu mới
- Mất kết nối TCP → session bị huỷ

---

## 8. Xử lý lỗi

- Không retry
- Không resume
- Lỗi socket hoặc protocol → coi như `CANCEL`
- Ưu tiên đơn giản và dễ debug

---

## 9. Bảo mật

- Không mã hoá ở tầng ứng dụng
- Khi chạy trong Tailscale / VPN → đã được mã hoá sẵn
- Không xác thực người dùng

---

## 10. Nguyên tắc thiết kế

- Protocol đơn giản, dễ đọc, dễ debug
- Độc lập với UI (GUI / TUI)
- Có thể mở rộng trong tương lai (checksum, encryption, resume)

---

## 11. Phiên bản

- Protocol version: `v1`
- Mọi thay đổi protocol phải được cập nhật trong file này
