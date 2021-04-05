import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel implements Runnable {

    enum ChannelType { MC, MDB, MDR }

    private MulticastSocket multicastSocket;
    private InetAddress inetAddress;
    private ChannelType type;
    private int BUF_SIZE;
    private int port;

    public Channel(String addressString, int port, ChannelType type, int chunkSize) throws IOException {
        this.inetAddress = InetAddress.getByName(addressString);
        this.port = port;
        this.type = type;

        // "Creating" Channel itself
        this.multicastSocket = new MulticastSocket(this.port);
        this.multicastSocket.setTimeToLive(1); // Set the default time-to-live for multicast packets sent out on this MulticastSocket in order to control the scope of the multicasts.
        this.multicastSocket.joinGroup(this.inetAddress);
    }

    public void channelSendMessage(String content) {
        DatagramPacket packetToSend = new DatagramPacket(content.getBytes(), content.getBytes().length, this.inetAddress, this.port);

        try {
            this.multicastSocket.send(packetToSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] deliveredData = new byte[BUF_SIZE];
        DatagramPacket deliveredPacket = new DatagramPacket(deliveredData, deliveredData.length);

        while (true) {

            try {
                this.multicastSocket.receive(deliveredPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
