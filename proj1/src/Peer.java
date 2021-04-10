import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

public class Peer implements RemoteInterface {
    /**
     * Singleton instance of Peer
     */
    public static Peer instance;
    private static FileStorage fileStorage;

    /**
     * Peers working directory
     */
    public static String serviceDirectory;

    private String protocolVersion;
    private static int peerID;
    public static String accessPoint;

    private static Channel MC;
    private static Channel MDB;
    private static Channel MDR;

    private static ScheduledThreadPoolExecutor exec;
//    private ScheduledExecutorService executor;

    public static void main(String[] args) throws IOException, AlreadyBoundException {
        if(args.length != 9) {
            System.out.println("Usage: Java Peer <protocol version> <peer id> " +
                    "<service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> " +
                    "<MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
            return;
        }

        new Peer(args);
        fileStorage = FileStorage.loadFromDisk();

        // RMI Connection
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(instance, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.bind(accessPoint, stub);

        // Exit handler ; Unbinds RMI and saves storage
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler(registry)));

        // Schedule saving to file at 5 seconds rate
        exec.scheduleAtFixedRate(fileStorage::saveToDisk, 0, 5, TimeUnit.SECONDS);

        System.out.println("Peer ready");
    }

    public Peer(String[] args) throws IOException {
        if (Peer.instance != null) return;
        else Peer.instance = this;

        this.protocolVersion = args[0];
        peerID = Integer.parseInt(args[1]);
        accessPoint = args[2];
        serviceDirectory = "service-" + peerID;

        exec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(200);;

        MC = new Channel(args[3], Integer.parseInt(args[4]), Channel.ChannelType.MC);
        MDB = new Channel(args[5], Integer.parseInt(args[6]), Channel.ChannelType.MDB);
        MDR = new Channel(args[7], Integer.parseInt(args[8]), Channel.ChannelType.MDR);

        exec.execute(MC);
        exec.execute(MDB);
        exec.execute(MDR);
    }

    public static String getServiceDirectory() {
        return serviceDirectory;
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

        fileStorage.initiateBackup(fileParser);
        fileStorage.saveToDisk();
    }

    public void restore(String filepath) {
        File targetFile = new File(Restore.extractFileNameFromPath(filepath));
        if (targetFile.exists() && !targetFile.isDirectory()){
            System.out.println("Already restored a file by that name ; Aborting");
            return;
        }

        System.out.println("RESTORE SERVICE -> FILE PATH = " + filepath);

        FileParser fileParser = new FileParser(filepath);
        long fileSize = fileParser.getFile().length();
        int numberOfChunksToFind = ((int)fileSize / FileParser.MAX_CHUNK_SIZE) + 1;

        for (int i = 0; i < numberOfChunksToFind; i++) {
            String messageString = this.protocolVersion + " GETCHUNK " + peerID + " " + fileParser.getFileID() + " " + i + " " + "\r\n" + "\r\n";
            byte[] messageBytes = messageString.getBytes();

            MC.sendMessage(messageBytes);
        }

        Restore.t = Peer.getExec().scheduleWithFixedDelay(() -> Restore.constructRestoredFileFromRestoredChunks(numberOfChunksToFind, filepath, fileParser.getFileID()), 100, 100, TimeUnit.MILLISECONDS);
        fileStorage.saveToDisk();
    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);

        FileParser fileParser = new FileParser(filepath);

        String messageString = this.protocolVersion  + " DELETE " + peerID + " " + fileParser.getFileID() + " " + "\r\n" + "\r\n";
        byte[] messageBytes = messageString.getBytes();

        fileStorage.removeInitiatedFile(fileParser);

        System.out.println("Sending Message to MC");
        MC.sendMessage(messageBytes);
        fileStorage.saveToDisk();
    }

    public void reclaim(long spaceReclaim) {
        System.out.println("RECLAIM SERVICE -> DISK SPACE RECLAIM = " + spaceReclaim);
        // TODO:
        // saveFileStorageToDisk();
    }

    public String state() {
        return fileStorage.toString();
    }

    public static ScheduledThreadPoolExecutor getExec() {
        return exec;
    }

    public static int getId() {
        return peerID;
    }

    public static Channel getMDB() {
        return MDB;
    }
    public static Channel getMC() {
        return MC;
    }
    public static Channel getMDR() {
        return MDR;
    }
}