import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
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
    private static FileStorage storage;
//    private ScheduledExecutorService executor;

    public static void main(String args[]) throws IOException, AlreadyBoundException {
        if(args.length != 9) {
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> " +
                    "<MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
            return;
        }

        Peer.instance = new Peer(args);

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



    public synchronized void backup(String filepath, int replicationDegree) {
        System.out.println("\n---- BACKUP SERVICE ---- FILE PATH = " + filepath + " | REPLICATION DEGREEE = " + replicationDegree);

        FileParser fileParser = new FileParser(filepath, replicationDegree);
        // storage.addFile(fileParser);

        System.out.println(fileParser.getChunks().size());

        for (Chunk c : fileParser.getChunks()) {

            String dataHeader = this.protocolVersion + " PUTCHUNK " + peerID + " " + fileParser.getId() + " " + c.getNr() + " " + replicationDegree + " " + "\r\n" + "\r\n";
            System.out.println(dataHeader);

            // System.out.println(Arrays.toString(c.getContent()));

            byte[] fullMessage = new byte[dataHeader.length() + c.getContent().length];
            System.arraycopy(dataHeader.getBytes(), 0, fullMessage,0, dataHeader.getBytes().length);
            System.arraycopy(c.getContent(), 0, fullMessage, dataHeader.getBytes().length, c.getContent().length);

            System.out.println("Sending Message to MDB");
            MDB.sendMessage(fullMessage);

            Peer.getExec().schedule(new CheckReplicationDegree(fullMessage, fileParser.getId(), c.getNr(), replicationDegree), 1, TimeUnit.SECONDS);
        }
    }

    public void restore(String filepath) {
        System.out.println("RESTORE SERVICE -> FILE PATH = " + filepath);
        // TODO:
    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);
        // TODO:
    }

    public void reclaim(long spaceReclaim) {
        System.out.println("RECLAIM SERVICE -> DISK SPACE RECLAIM = " + spaceReclaim);
        // TODO:
    }

    public String state() {
        System.out.println("\n---- STATE SERVICE ----");
        return "";
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

    public static FileStorage getStorage() {
        return storage;
    }

    public static Channel getMDB() {
        return MDB;
    }
    public static Channel getMC() {
        return MC;
    }

    //    public void send(String[] args) {
//
//        String answer;
//
//        if (args[1].equals("PUTCHUNK")) {
//            answer = args[0] + " STORED " + args[2] + " " + args[3] + " " + args[4] + " " + "\r\n" + "\r\n";
//            System.out.println("Answer: " + answer);
//            byte[] answerBytes = answer.getBytes();
//
//            DatagramPacket packet = new DatagramPacket(answerBytes, answerBytes.length, MC);
//
//            try {
//                this.socket.send(packet);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}