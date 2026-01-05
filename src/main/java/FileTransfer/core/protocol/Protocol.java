package FileTransfer.core.protocol;

public final class Protocol{
    // network ports
    public static final int UDP_PORT = 50000;
    public static final int TCP_PORT = 50001;

    //timeouts and intervals (in milliseconds)
    public static final int DISCOVER_INTERVAL = 3000;
    public static final int DISCOVER_TIMEOUT = 10000;

    // message format
    public static final String FIELD_SEPARATOR = "|";
    public static final String LINE_SEPARATOR = "\n";

    private Protocol() {
        // Prevent instantiation
    }

    // message builders and parsers can be added here

    public static String buildMessage(MessageType type, String... fields) {
        StringBuilder message = new StringBuilder(type.name());
        for (String field : fields) {
            message.append(FIELD_SEPARATOR).append(field);
        }
        message.append(LINE_SEPARATOR);
        return message.toString();
    }

    // Message builders
    public static String buildDiscover(String peerId, String peerName, int tcpPort) {
        return buildMessage(MessageType.DISCOVER, peerId, peerName, String.valueOf(tcpPort));
    }
    
    public static String buildHello(String peerId, String deviceName) {
        return buildMessage(MessageType.HELLO, peerId, deviceName);
    }
    
    public static String buildSendRequest(String filename, long filesize) {
        return buildMessage(MessageType.SEND_REQUEST, filename, String.valueOf(filesize));
    }
    
    public static String buildSendAccept() {
        return MessageType.SEND_ACCEPT.name() + LINE_SEPARATOR;
    }
    
    public static String buildSendReject() {
        return MessageType.SEND_REJECT.name() + LINE_SEPARATOR;
    }
    
    public static String buildStartSend() {
        return MessageType.START_SEND.name() + LINE_SEPARATOR;
    }
    
    public static String buildCancel() {
        return MessageType.CANCEL.name() + LINE_SEPARATOR;
    }
    
    public static String buildDone() {
        return MessageType.DONE.name() + LINE_SEPARATOR;
    }
    
    public static String buildError(String errorMessage) {
        return buildMessage(MessageType.ERROR, errorMessage);
    }
}
