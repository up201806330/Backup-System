import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;

public class Message implements Runnable {

    enum MessageType { PUTCHUNK, STORED, GETCHUNK, CHUNK, DELETE, REMOVED }

    public byte CR = 0xD;
    public byte LF = 0xA;
    public String CRLF = Byte.toString(CR) + Byte.toString(LF); // not sure if this works

    private String version;
    private MessageType msgType;
    private int senderId;
    private String fileId;
    private int chunkNo;
    private int replicationDeg;

    private int headerLength;
    private byte[] messageBodyData;

    public Message(DatagramPacket packet) {

        try {
            parseMessageHeader(packet.getData());
        } catch (IOException e) {
            System.out.println("Error in message header parsing\n");
            e.printStackTrace();
        }

        if (msgType == MessageType.PUTCHUNK || msgType == MessageType.CHUNK) {
            // Source: https://stackoverflow.com/questions/18367539/slicing-byte-arrays-in-java/18367574
            messageBodyData = Arrays.copyOfRange(packet.getData(), headerLength + 2 * CRLF.length(), packet.getLength());
            // lower limit = headerLength + 2 * CRLF.length() because header ends with 2 CRLFs
        }

    }

    public Message(MessageType type, String[] args) {
        this.msgType = type;

        // https://stackoverflow.com/questions/14256013/add-string-to-beginning-of-string-array
        ArrayList<String> temp = new ArrayList<String>();
        temp.add(this.msgType.toString()); temp.addAll(Arrays.asList(args));
        String[] newArgs = temp.toArray(new String[temp.size()]);

        switch (this.msgType.toString()) {
            case "PUTCHUNK" -> putChunkCase(newArgs);
            case "DELETE" -> deleteCase(newArgs);
            case "STORED", "GETCHUNK", "CHUNK", "REMOVED" -> generalCase(newArgs);
        }
    }


    private void parseMessageHeader(byte[] header) throws IOException {
        ByteArrayInputStream headerStream = new ByteArrayInputStream(header);
        BufferedReader reader = new BufferedReader(new InputStreamReader(headerStream));

        String headerLine = reader.readLine();
        headerLength = headerLine.getBytes().length; // TODO not sure if it is correct

        if (!headerArgsParsing(headerLine)) {
            System.out.println("Unsuccessful Header Parsing");
        }
    }

    private boolean headerArgsParsing(String headerLine) {
        String removedExtraSpaces = headerLine.trim(); // because "there may be zero or more spaces after the last field in a line"
        String[] splitHeader = removedExtraSpaces.split(" "); // TODO only white space is allowed, right?

        switch (splitHeader[1]) {
            case "PUTCHUNK":
                return putChunkCase(splitHeader);
            case "DELETE":
                return deleteCase(splitHeader);
            case "STORED":
            case "GETCHUNK":
            case "CHUNK":
            case "REMOVED":
                return generalCase(splitHeader);
        }

        System.out.println("Header Parsing Unsuccessful");
        return false;
    }

    private boolean putChunkCase(String[] splitHeader) {
        if (splitHeader.length != 6) { return false; }
        else {
            version = splitHeader[0];
            senderId = Integer.parseInt(splitHeader[2]);
            fileId = splitHeader[3];
            chunkNo = Integer.parseInt(splitHeader[4]);
            replicationDeg = Integer.parseInt(splitHeader[5]);

            return true;
        }
    }

    private boolean deleteCase(String[] splitHeader) {
        if (splitHeader.length != 4) { return false; }
        else {
            version = splitHeader[0];
            senderId = Integer.parseInt(splitHeader[2]);
            fileId = splitHeader[3];

            return true;
        }
    }

    private boolean generalCase(String[] splitHeader) {
        if (splitHeader.length != 5) { return false; }
        else {
            version = splitHeader[0];
            senderId = Integer.parseInt(splitHeader[2]);
            fileId = splitHeader[3];
            chunkNo = Integer.parseInt(splitHeader[4]);

            return true;
        }
    }

    public String getVersion() {
        return version;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getReplicationDeg() {
        return replicationDeg;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public byte[] getMessageBodyData() {
        return messageBodyData;
    }


    @Override
    public void run() {

        switch (this.msgType) {
            case PUTCHUNK:
                if (this.senderId != Peer.getPeer().getPeerId()) {
                    new Backup(this);
                }
            case STORED:
                if (this.senderId != Peer.getPeer().getPeerId()) {
                    // TODO
                }

        }
    }

}
