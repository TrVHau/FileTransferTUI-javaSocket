package FileTransfer.core.network;

import FileTransfer.core.protocol.MessageType;

public class MessageParser {
    public static ParsedMessage parse(String raw) throws IllegalArgumentException {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("Empty message");
        }
        
        String[] parts = raw.trim().split("\\|");
        if(parts.length == 0) {
            throw new IllegalArgumentException("Invalid message format");
        }
        MessageType type;
        try {
            type = MessageType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown message type: " + parts[0]);
        }

        String[] fields = new String[parts.length - 1];
        System.arraycopy(parts, 1, fields, 0, parts.length - 1);
        validate(type, fields);
        return new ParsedMessage(type, fields);
    }
     
    public static void validate(MessageType type, String[] fields) throws IllegalArgumentException {
        switch (type) {
            case DISCOVER:
                if (fields.length != 3) {
                    throw new IllegalArgumentException("DISCOVER requires 3 fields: peer_id, device_name, tcp_port");
                }
                break;
            case HELLO:
                if (fields.length != 2) {
                    throw new IllegalArgumentException("HELLO requires 2 fields: peer_id, device_name");
                }
                break;
            case SEND_REQUEST:
                if (fields.length != 2) {
                    throw new IllegalArgumentException("SEND_REQUEST requires 2 fields: filename, filesize");
                }
                break;
            case SEND_ACCEPT:
            case SEND_REJECT:
            case START_SEND:
            case CANCEL:
            case DONE:
                if (fields.length != 0) {
                    throw new IllegalArgumentException(type.name() + " should have no fields");
                }
                break;
            case ERROR:
                // ERROR can have optional message
                break;
            default:
                break;
        }
    }

    public static class ParsedMessage {
        public MessageType type;
        public String[] fields;

        public ParsedMessage(MessageType type, String[] fields) {
            this.type = type;
            this.fields = fields;
        }
    }
}
