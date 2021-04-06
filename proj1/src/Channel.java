import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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


    @Override
    public void run() {
        byte[] deliveredData = new byte[BUF_SIZE];
        DatagramPacket deliveredPacket = new DatagramPacket(deliveredData, deliveredData.length);

        while (true) {
            try {
                this.multicastSocket.receive(deliveredPacket);
                ScheduledThreadPoolExecutor peerPool =  Peer.getPeerObject().getPool();
                peerPool.execute(new Message(deliveredPacket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
