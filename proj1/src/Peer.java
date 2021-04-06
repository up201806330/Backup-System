import java.io.IOException;
import java.net.DatagramSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Peer implements RemoteInterface {


    private static Peer singleton;

    private int peerId;
    private String protocolVersion;
    private static String accessPoint;

    private DatagramSocket socket;
    private HashMap<ServiceChannel.ChannelType, ServiceChannel> channels;

    private ScheduledThreadPoolExecutor pool;
    private ScheduledExecutorService executor;


    public static void main(String[] args) throws IOException, AlreadyBoundException {

        if (args.length != 9) {
            System.out.println("Usage: Java Peer <protocol_version> <peer_id> <service_access_point> <MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>");
            return ;
        }

        Peer.singleton = new Peer(args);
    }

    public Peer(String args[]) throws IOException, AlreadyBoundException {

        this.protocolVersion = args[0];
        this.peerId = Integer.parseInt(args[1]);
        accessPoint = args[2];

        this.socket = new DatagramSocket();
        this.pool = new ScheduledThreadPoolExecutor(100);
        this.executor = Executors.newScheduledThreadPool(1);

        configChannels(args);

        connectRemoteInterface();
    }


    public static void connectRemoteInterface() throws RemoteException, AlreadyBoundException {
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(Peer.singleton, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(accessPoint, stub);
    }

    public void configChannels(String[] args) throws IOException {
        ServiceChannel MC  = new ServiceChannel(args[3], Integer.parseInt(args[4]), ServiceChannel.ChannelType.MC, Chunk.CHUNK_MAX_SIZE);
        new Thread(MC).start();

        ServiceChannel MDB = new ServiceChannel(args[5], Integer.parseInt(args[6]), ServiceChannel.ChannelType.MDB,Chunk.CHUNK_MAX_SIZE);
        new Thread(MDB).start();

        ServiceChannel MDR = new ServiceChannel(args[7], Integer.parseInt(args[8]), ServiceChannel.ChannelType.MDR, Chunk.CHUNK_MAX_SIZE);
        new Thread(MDR).start();
    }

    @Override
    public void backup(String filePathname, int replicationDegree) throws RemoteException {
        System.out.println("Entering Backup of " + filePathname + " with repDegree of " + replicationDegree);

        Backup backup = new Backup(filePathname, replicationDegree);

        backup.run();
    }

    @Override
    public void restore(String filePathname) throws RemoteException {
        System.out.println("Entering Restore of " + filePathname);
    }

    @Override
    public void delete(String filePathname) throws RemoteException {
        System.out.println("Entering Delete of " + filePathname);
    }

    @Override
    public void reclaimStorage(int maxDiskSpaceToStore) throws RemoteException {
        System.out.println("Entering Reclaim with disk space reclaim of " + maxDiskSpaceToStore);
    }

    @Override
    public String retrieveState() throws RemoteException {
        System.out.println("Entering Retrieve of State");

        return "";
    }

    public static Peer getPeer() {
        return singleton;
    }

    public int getPeerId() {
        return peerId;
    }

    public ScheduledThreadPoolExecutor getPool() {
        return pool;
    }

}
