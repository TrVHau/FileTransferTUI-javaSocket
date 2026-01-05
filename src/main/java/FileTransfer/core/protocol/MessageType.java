package FileTransfer.core.protocol;

public enum MessageType {
    // UDP Discovery
    DISCOVER,
    
    // TCP Control Protocol
    HELLO,
    SEND_REQUEST,
    SEND_ACCEPT,
    SEND_REJECT,
    START_SEND,
    CANCEL,
    DONE,
    ERROR
}
