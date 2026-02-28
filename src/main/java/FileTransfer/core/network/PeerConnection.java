package FileTransfer.core.network;

import FileTransfer.core.protocol.Protocol;
import FileTransfer.core.transfer.TransferCallback;
import FileTransfer.core.util.FormatUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles a single TCP connection with a remote peer.
 * Implements the state machine described in the protocol spec.
 *
 * Uses BufferedInputStream (not BufferedReader) so that control-message
 * parsing and raw-byte file streaming share the same buffer safely.
 */
public class PeerConnection implements Runnable {

    private final Socket socket;
    private BufferedInputStream bis;
    private OutputStream out;
    private TransferCallback callback;

    private enum State {
        IDLE,
        WAITING_FOR_RESPONSE,
        WAITING_FOR_START,
        SENDING,
        WAITING_FOR_DONE,
        RECEIVING,
        COMPLETED,
        ERROR
    }

    private volatile State currentState = State.IDLE;

    private final String localPeerId;
    private final String localPeerName;
    private String remotePeerId;
    private String remoteDeviceName;
    private String pendingFilename;
    private long pendingFilesize;
    private File fileToSend;
    private String downloadPath = "downloads";

    private final CountDownLatch helloLatch = new CountDownLatch(1);
    private static final int BUFFER_SIZE = 65536;
    private static final int MAX_LINE_LENGTH = 8192;
    private static final int HANDSHAKE_TIMEOUT_SEC = 10;
    private static final int SOCKET_TIMEOUT_MS = 60_000;

    public PeerConnection(Socket socket, TransferCallback callback,
                          String localPeerId, String localPeerName) {
        this.socket = socket;
        this.callback = callback;
        this.localPeerId = localPeerId;
        this.localPeerName = localPeerName;
    }

    public void setDownloadPath(String path)       { this.downloadPath = path; }
    public void setCallback(TransferCallback cb)   { this.callback = cb; }

    // ────────────────── Main loop ──────────────────

    @Override
    public void run() {
        try {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            bis = new BufferedInputStream(socket.getInputStream());
            out = socket.getOutputStream();

            sendHello();

            String line;
            while ((line = readLine()) != null) {
                handleMessage(line);
                if (currentState == State.COMPLETED || currentState == State.ERROR) break;
            }
        } catch (IOException e) {
            if (currentState == State.SENDING || currentState == State.RECEIVING) {
                notifyError("Connection lost: " + e.getMessage());
            }
            currentState = State.ERROR;
        } finally {
            cleanup();
        }
    }

    /** Read one protocol line (\n terminated) from the byte stream. */
    private String readLine() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
        int b;
        while ((b = bis.read()) != -1) {
            if (b == '\n') return buf.toString(StandardCharsets.UTF_8);
            if (b == '\r') continue;
            if (buf.size() > MAX_LINE_LENGTH) {
                throw new IOException("Protocol line exceeds maximum length");
            }
            buf.write(b);
        }
        return buf.size() > 0 ? buf.toString(StandardCharsets.UTF_8) : null;
    }

    // ────────────────── Message dispatch ──────────────────

    private void handleMessage(String message) {
        try {
            MessageParser.ParsedMessage parsed = MessageParser.parse(message);
            switch (parsed.type) {
                case HELLO        -> handleHello(parsed.fields);
                case SEND_REQUEST -> handleSendRequest(parsed.fields);
                case SEND_ACCEPT  -> handleSendAccept();
                case SEND_REJECT  -> handleSendReject();
                case START_SEND   -> handleStartSend();
                case CANCEL       -> handleCancel();
                case DONE         -> handleDone();
                case ERROR        -> handleError(parsed.fields);
                default           -> sendError("Unsupported message type");
            }
        } catch (Exception e) {
            sendError("Invalid message format");
        }
    }

    // ────────────────── Handlers ──────────────────

    private void handleHello(String[] fields) {
        if (fields.length < 2) { sendError("Invalid HELLO"); return; }
        remotePeerId = fields[0];
        remoteDeviceName = fields[1];
        helloLatch.countDown();
    }

    private void handleSendRequest(String[] fields) {
        if (fields.length < 2) { sendError("Invalid SEND_REQUEST"); return; }
        if (currentState != State.IDLE) { sendReject(); return; }

        pendingFilename = sanitizeFilename(fields[0]);
        try {
            pendingFilesize = Long.parseLong(fields[1]);
        } catch (NumberFormatException e) {
            sendError("Invalid file size");
            return;
        }

        boolean accepted = false;
        if (callback != null) {
            String name = remoteDeviceName != null ? remoteDeviceName : "Unknown";
            accepted = callback.onTransferRequest(name, socket.getInetAddress().getHostAddress(),
                    pendingFilename, pendingFilesize);
        }

        if (accepted) {
            sendAccept();
            currentState = State.WAITING_FOR_START;
        } else {
            sendReject();
            resetPending();
        }
    }

    private void handleSendAccept() {
        if (currentState != State.WAITING_FOR_RESPONSE) return;

        if (callback != null) callback.onTransferStart(pendingFilename, pendingFilesize, true);

        sendLine(Protocol.buildStartSend());
        currentState = State.SENDING;

        try {
            if (fileToSend == null || !fileToSend.exists()) {
                notifyError("File not found: " + (fileToSend != null ? fileToSend.getName() : "null"));
                sendLine(Protocol.buildCancel());
                currentState = State.ERROR;
                return;
            }
            socket.setSoTimeout(0);
            String checksum = streamFileOut();
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            currentState = State.WAITING_FOR_DONE;
        } catch (IOException e) {
            notifyError(e.getMessage());
            sendLine(Protocol.buildCancel());
            currentState = State.ERROR;
        }
    }

    private void handleSendReject() {
        notifyError("Transfer rejected by peer");
        currentState = State.IDLE;
        resetPending();
    }

    private void handleStartSend() {
        if (currentState != State.WAITING_FOR_START) return;

        if (callback != null) callback.onTransferStart(pendingFilename, pendingFilesize, false);
        currentState = State.RECEIVING;

        try {
            File downloadDir = new File(downloadPath);
            if (!downloadDir.exists()) downloadDir.mkdirs();

            File outputFile = new File(downloadPath, pendingFilename);
            socket.setSoTimeout(0);
            String checksum = streamFileIn(outputFile, pendingFilesize);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            sendLine(Protocol.buildDone());
            currentState = State.COMPLETED;

            if (callback != null) callback.onTransferComplete(pendingFilename, checksum);
        } catch (IOException e) {
            notifyError(e.getMessage());
            sendLine(Protocol.buildCancel());
            currentState = State.ERROR;
        }
    }

    private void handleCancel() {
        notifyError("Transfer cancelled by peer");
        currentState = State.IDLE;
        resetPending();
    }

    private void handleDone() {
        // Receiver confirmed full file receipt
        if (currentState == State.WAITING_FOR_DONE || currentState == State.SENDING) {
            currentState = State.COMPLETED;
            if (callback != null) {
                // We already streamed the file; compute checksum was done in streamFileOut
                callback.onTransferComplete(pendingFilename, lastSentChecksum);
            }
        }
    }

    private void handleError(String[] fields) {
        String msg = fields.length > 0 ? fields[0] : "Unknown error";
        notifyError(msg);
        currentState = State.ERROR;
    }

    // ────────────────── File I/O ──────────────────

    private String lastSentChecksum;

    private String streamFileOut() throws IOException {
        try (FileInputStream fis = new FileInputStream(fileToSend)) {
            MessageDigest md = getMD5();
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = fileToSend.length();
            long sent = 0;
            int read;
            long lastUpdate = 0;

            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                md.update(buffer, 0, read);
                sent += read;

                long now = System.currentTimeMillis();
                if (callback != null && (now - lastUpdate > 100 || sent == total)) {
                    callback.onProgress(sent, total, pendingFilename);
                    lastUpdate = now;
                }
            }
            out.flush();
            lastSentChecksum = FormatUtils.bytesToHex(md.digest());
            return lastSentChecksum;
        }
    }

    private String streamFileIn(File outputFile, long expectedBytes) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            MessageDigest md = getMD5();
            byte[] buffer = new byte[BUFFER_SIZE];
            long received = 0;
            long remaining = expectedBytes;
            long lastUpdate = 0;

            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = bis.read(buffer, 0, toRead);
                if (read == -1) {
                    throw new IOException("Stream ended early: received " + received + "/" + expectedBytes + " bytes");
                }
                fos.write(buffer, 0, read);
                md.update(buffer, 0, read);
                received += read;
                remaining -= read;

                long now = System.currentTimeMillis();
                if (callback != null && (now - lastUpdate > 100 || remaining == 0)) {
                    callback.onProgress(received, expectedBytes, pendingFilename);
                    lastUpdate = now;
                }
            }
            fos.flush();
            return FormatUtils.bytesToHex(md.digest());
        }
    }

    // ────────────────── Sender-side public API ──────────────────

    public void sendFileRequest(File file) {
        try {
            if (!helloLatch.await(HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                notifyError("Handshake timeout \u2014 peer did not respond");
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (currentState != State.IDLE) {
            notifyError("Connection is busy");
            return;
        }
        if (!file.exists() || !file.isFile()) {
            notifyError("File does not exist: " + file.getPath());
            return;
        }
        fileToSend = file;
        pendingFilename = file.getName();
        pendingFilesize = file.length();
        currentState = State.WAITING_FOR_RESPONSE;
        sendLine(Protocol.buildSendRequest(file.getName(), file.length()));
    }

    public void sendCancel() {
        sendLine(Protocol.buildCancel());
        currentState = State.IDLE;
    }

    // ────────────────── Helpers ──────────────────

    private void sendHello() {
        sendLine(Protocol.buildHello(localPeerId, localPeerName));
    }

    private void sendAccept()  { sendLine(Protocol.buildSendAccept()); }
    private void sendReject()  { sendLine(Protocol.buildSendReject()); }

    public void sendError(String msg) {
        sendLine(Protocol.buildError(msg));
        currentState = State.ERROR;
    }

    private void sendLine(String msg) {
        if (out != null) {
            try {
                out.write(msg.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                // connection lost — caught by main loop
            }
        }
    }

    private void notifyError(String msg) {
        if (callback != null && pendingFilename != null) {
            callback.onTransferError(pendingFilename, msg);
        }
    }

    private void resetPending() {
        pendingFilename = null;
        pendingFilesize = 0;
    }

    /** Strip path separators and leading dots to prevent directory traversal. */
    private static String sanitizeFilename(String name) {
        String safe = name.replace('/', '_').replace('\\', '_');
        while (safe.startsWith(".")) safe = safe.substring(1);
        return safe.isEmpty() ? "unnamed_file" : safe;
    }

    private static MessageDigest getMD5() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new IOException("MD5 not available", e);
        }
    }

    private void cleanup() {
        try { if (bis != null) bis.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }
}
