import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
    private static String serviceDirectory;

    private String protocolVersion;
    private static int peerID;
    public static String accessPoint;

    private static Channel MC;
    private static Channel MDB;
    private static Channel MDR;

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
        fileStorage = loadFileStorageFromDisk();

        // RMI Connection
        RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(instance, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(accessPoint, stub);

        // Exit handler ; Unbinds RMI and saves storage
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler(registry)));

        System.out.println("Peer ready");
    }


    public Peer(String args[]) throws IOException {
        if (Peer.instance != null) return;
        else Peer.instance = this;

        this.protocolVersion = args[0];
        peerID = Integer.parseInt(args[1]);
        accessPoint = args[2];
        serviceDirectory = "service-" + peerID;

        exec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(200);;
//        this.executor = Executors.newScheduledThreadPool(1);

        MC = new Channel(args[3], Integer.parseInt(args[4]), Channel.ChannelType.MC);
        MDB = new Channel(args[5], Integer.parseInt(args[6]), Channel.ChannelType.MDB);
        MDR = new Channel(args[7], Integer.parseInt(args[8]), Channel.ChannelType.MDR);

        exec.execute(MC);
        exec.execute(MDB);
        exec.execute(MDR);

//        new Thread(this.MC).start();
//        new Thread(this.MDB).start();
//        new Thread(this.MDR).start();

//        channels = new HashMap<>();
//        channels.put(Channel.ChannelType.MC, MC);
//        channels.put(Channel.ChannelType.MDB, MDB);
//        channels.put(Channel.ChannelType.MDR, MDR);
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
        saveFileStorageToDisk();
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

        System.out.println("Number of chunks to find: " + numberOfChunksToFind);

        for (int i = 1; i < numberOfChunksToFind + 1; i++) {
            String messageString = this.protocolVersion + " GETCHUNK " + peerID + " " + fileParser.getFileID() + " " + i + " " + "\r\n" + "\r\n";
            byte[] messageBytes = messageString.getBytes();

            System.out.println("Sending Message to MC");
            MC.sendMessage(messageBytes);
        }

        Restore.t = Peer.getExec().scheduleWithFixedDelay(() -> Restore.constructRestoredFileFromRestoredChunks(numberOfChunksToFind, filepath, fileParser.getFileID()), 100, 100, TimeUnit.MILLISECONDS);
        System.out.println("Ending Restored. Proceeding to saveFileStorageToDisk");
        saveFileStorageToDisk();
    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);

        FileParser fileParser = new FileParser(filepath);

        String messageString = this.protocolVersion  + " DELETE " + peerID + " " + fileParser.getFileID() + " " + "\r\n" + "\r\n";
        byte[] messageBytes = messageString.getBytes();

        fileStorage.removeInitiatedFile(fileParser);

        System.out.println("Sending Message to MC");
        MC.sendMessage(messageBytes);
        saveFileStorageToDisk();
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

    public String getVersion() {
        return protocolVersion;
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

    public static void saveFileStorageToDisk(){
        try{
            FileOutputStream fs = new FileOutputStream(serviceDirectory + "/" + "State");
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(fileStorage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static FileStorage loadFileStorageFromDisk() throws IOException {
        try{
            FileInputStream fs = new FileInputStream(serviceDirectory + "/" + "State");
            ObjectInputStream os = new ObjectInputStream(fs);
            FileStorage.instance = (FileStorage) os.readObject();
            return FileStorage.instance;
        } catch (FileNotFoundException e){
            System.out.println("File Storage not found ; Creating new one");
            return new FileStorage();
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
            return new FileStorage();
        }
    }
}