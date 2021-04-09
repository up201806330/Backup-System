import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Channel implements Runnable {
    enum ChannelType { MC, MDB, MDR };

    private InetAddress inetAddress;
    private final int port;
    private final ChannelType type;
    private final MulticastSocket socket;


    public Channel(String addressString, int port, ChannelType type) throws IOException {
        try {
            this.inetAddress = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.port = port;
        this.type = type;

        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.inetAddress);

        System.out.println("Exiting Channel Constructor");
    }

    @Override
    public void run() {

        System.out.println("Entering Channel Run");

        byte[] buffer = new byte[80000]; // 80000 > 64000

        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                this.socket.receive(receivedPacket);
                System.out.println("Packet Received");

            } catch (IOException e) {
                e.printStackTrace();
            }

            processPacket(receivedPacket);
        }
    }

    private void processPacket(DatagramPacket receivedPacket) {
        int size = receivedPacket.getLength();
        byte[] data = Arrays.copyOf(receivedPacket.getData(), size);

        int i;
        for (i = 0; i < data.length; i++) {
            if (data[i] == 0xD && data[i + 1] == 0xA && data[i + 2] == 0xD && data[i + 3] == 0xA) break;
        }

        byte[] header = Arrays.copyOfRange(data, 0, i);
        String[] splitHeader = new String(header).trim().split(" ");

        String command = splitHeader[1];
        String fileID = splitHeader[3];
        int chunkNr = splitHeader.length >= 5 ? Integer.parseInt(splitHeader[4]) : 0;
        int desiredRepDegree = splitHeader.length == 6 ? Integer.parseInt(splitHeader[5]) : 0;

        Chunk newChunk = new Chunk(fileID, chunkNr, desiredRepDegree);
        switch (command) {
            case "PUTCHUNK":
                byte[] body = Arrays.copyOfRange(data, i + 4, data.length);
                newChunk.setContent(body);
                Backup.processPacketPUTCHUNK(newChunk, splitHeader);
                break;
            case "STORED":
                Backup.processPacketSTORED(newChunk);
                break;
            case "DELETE":
                Delete.processPacketDELETE(splitHeader[3]);
                break;
            default:
                break;
        }
    }

    public synchronized void sendMessage(byte[] buf) {
        try {
            this.socket.send(new DatagramPacket(buf, buf.length, this.inetAddress, this.port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public int getPort() {
        return port;
    }

    public ChannelType getType() {
        return type;
    }
}
