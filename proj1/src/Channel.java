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
    private int port;
    private ChannelType type;
    private MulticastSocket socket;


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

            processPacket(receivedPacket, type);

        }
    }

    private void processPacket(DatagramPacket receivedPacket, ChannelType type) {

        int size = receivedPacket.getLength();
        byte[] data = Arrays.copyOf(receivedPacket.getData(), size);

        int i;
        for (i = 0; i < data.length; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') break;
        }

        byte[] header = Arrays.copyOfRange(data, 0, i);
        String[] splitHeader = new String(header).trim().split(" ");

        // splitHeader[1] -> PUTCHUNK / etc .....
        switch (splitHeader[1]) {
            case "PUTCHUNK":
                byte[] body = Arrays.copyOfRange(data, i + 4, data.length);
                String bodyString = new String(body);
                processPacketPUTCHUNK(splitHeader, bodyString);
                break;
            case "STORED":
                processPacketSTORED(splitHeader);
                break;
            default:
                System.out.println("rip");
        }
    }

    private void processPacketPUTCHUNK(String[] splitHeader, String bodyString) {
        System.out.println("Processing PUTCHUNK Packet");

        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {

            System.out.println("SKIPPING PUTCHUNK");
            return ;
        }
        printSplitHeader(splitHeader);

        String name = splitHeader[3] + "-" + splitHeader[4];

        try {
            FileOutputStream fos = new FileOutputStream(name);
            fos.write(bodyString.getBytes());
            fos.close();

            byte[] storedMessage = createSTORED(splitHeader);
            Peer.getMC().sendMessage(storedMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processPacketSTORED(String[] splitHeader) {
        System.out.println("Processing STORED Packet");
        
    }

    private byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + splitHeader[2] + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }


    private void printSplitHeader(String[] splitHeader) {
        System.out.println("Length of split Header: " + splitHeader.length);
        System.out.println("Version : " + splitHeader[0]);
        System.out.println("Command : " + splitHeader[1]);
        System.out.println("SenderId: " + splitHeader[2]);
        System.out.println("FileId  : " + splitHeader[3]);
        System.out.println("ChunkNr : " + splitHeader[4]);
        System.out.println("RepDegr : " + splitHeader[5]);
    }

    public synchronized void sendMessage(byte[] buf) {

        try {
            System.out.println("-- Sending Message");
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
