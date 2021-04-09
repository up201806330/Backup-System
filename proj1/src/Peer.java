import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

public class Peer implements RemoteInterface {
    public static Peer instance;

    private String protocolVersion;
    private static int peerID;
    private static String accessPoint;

    private static Channel MC;
    private static Channel MDB;
    private static Channel MDR;

    private DatagramSocket socket;

    private static ScheduledThreadPoolExecutor exec;
//    private ScheduledExecutorService executor;

    public static void main(String args[]) throws IOException, AlreadyBoundException {
        if(args.length != 9) {
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> " +
                    "<MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
            return;
        }

        new Peer(args);
        new FileStorage();

        // RMI Connection
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(instance, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(args[2], stub); // args[2] -> accessPoint

        System.out.println("Peer ready");
    }


    public Peer(String args[]) throws IOException {
        if (Peer.instance != null) return;
        else Peer.instance = this;

        this.protocolVersion = args[0];
        this.peerID = Integer.parseInt(args[1]);
        this.accessPoint = args[2];

        this.exec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(200);;
//        this.executor = Executors.newScheduledThreadPool(1);

        this.socket = new DatagramSocket();

        this.MC = new Channel(args[3], Integer.parseInt(args[4]), Channel.ChannelType.MC);
        this.MDB = new Channel(args[5], Integer.parseInt(args[6]), Channel.ChannelType.MDB);
        this.MDR = new Channel(args[7], Integer.parseInt(args[8]), Channel.ChannelType.MDR);

        exec.execute(this.MC);
        exec.execute(this.MDB);
        exec.execute(this.MDR);

//        new Thread(this.MC).start();
//        new Thread(this.MDB).start();
//        new Thread(this.MDR).start();

//        channels = new HashMap<>();
//        channels.put(Channel.ChannelType.MC, MC);
//        channels.put(Channel.ChannelType.MDB, MDB);
//        channels.put(Channel.ChannelType.MDR, MDR);
    }

    public synchronized void backup(String filepath, int replicationDegree) throws Exception {
        if (replicationDegree > 9) {
            replicationDegree = 9;
            System.out.println("Replication degree capped to 9");
        }

        System.out.println("\n---- BACKUP SERVICE ---- FILE PATH = " + filepath + " | REPLICATION DEGREEE = " + replicationDegree);

        FileParser fileParser = new FileParser(filepath, replicationDegree);
        if (fileParser.getFile().length() > 64000000){
            throw new Exception("File size bigger than 64GB ; Aborting...");
        }

        for (Chunk chunk : fileParser.getChunks()) {

            String dataHeader = this.protocolVersion + " PUTCHUNK " + peerID + " " + fileParser.getFileID() + " " + chunk.getChunkNumber() + " " + replicationDegree + " " + "\r\n" + "\r\n";
            System.out.println(dataHeader);

            byte[] fullMessage = new byte[dataHeader.length() + chunk.getContent().length];
            System.arraycopy(dataHeader.getBytes(), 0, fullMessage,0, dataHeader.getBytes().length);
            System.arraycopy(chunk.getContent(), 0, fullMessage, dataHeader.getBytes().length, chunk.getContent().length);

            MDB.sendMessage(fullMessage);

            Peer.getExec().schedule(new CheckReplicationDegree(fullMessage, chunk), 1, TimeUnit.SECONDS);
        }

        FileStorage.instance.backupFile(fileParser);
    }

    public void restore(String filepath) {
        System.out.println("RESTORE SERVICE -> FILE PATH = " + filepath);
        // TODO:
    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);

        FileParser fileParser = new FileParser(filepath);

        String messageString = this.protocolVersion  + " DELETE " + peerID + " " + fileParser.getFileID() + " " + "\r\n" + "\r\n";
        byte[] messageBytes = messageString.getBytes();

        System.out.println("Sending Message to MC");
        MC.sendMessage(messageBytes);
    }

    public void reclaim(long spaceReclaim) {
        System.out.println("RECLAIM SERVICE -> DISK SPACE RECLAIM = " + spaceReclaim);
        // TODO:
    }

    public String state() {
        return FileStorage.instance.toString();
    }

    public static ScheduledThreadPoolExecutor getExec() {
        return exec;
    }

    public static int getId() {
        return peerID;
    }

    public String getVersion() {
        return protocolVersion;
    }

    public static Channel getMDB() {
        return MDB;
    }
    public static Channel getMC() {
        return MC;
    }
}