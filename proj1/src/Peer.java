import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public static String protocolVersion;
    private static int peerID;
    public static String accessPoint;

    private static Channel MC;
    private static Channel MDB;
    private static Channel MDR;

    private static ScheduledThreadPoolExecutor exec;
    public static Set<ScheduledFuture<?>> futures = ConcurrentHashMap.newKeySet();

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
        exec.scheduleAtFixedRate(FileStorage::saveToDisk, 0, 5, TimeUnit.SECONDS);

        System.out.println("Peer ready");
    }

    public Peer(String[] args) throws IOException {
        if (Peer.instance != null) return;
        else Peer.instance = this;

        protocolVersion = args[0];
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

    public void backup(String filepath, int replicationDegree) throws Exception {
        if (replicationDegree > 9) {
            replicationDegree = 9;
            System.out.println("Replication degree capped to 9");
        }

        System.out.println("\n---- BACKUP SERVICE ---- FILE PATH = " + filepath + " | REPLICATION DEGREEE = " + replicationDegree);

        FileObject fileObject = new FileObject(filepath, replicationDegree);
        if (fileObject.getFile().length() > 64000000){
            throw new Exception("File size bigger than 64GB ; Aborting...");
        }

        fileStorage.initiateBackup(fileObject);

        futures.clear();
        for (Chunk chunk : fileObject.getChunks()) {
            System.out.println("CHunk nr" + chunk.getChunkNumber());
            futures.add(initiatePUTCHUNK(fileObject.getFileID(), chunk));
        }

        AtomicBoolean failed = new AtomicBoolean(false);
        final Set<ScheduledFuture<?>> processedFutures = new HashSet<>();
        Set<ScheduledFuture<?>> futuresToProcess;
        do{
            futuresToProcess = new HashSet<>(futures);
            futuresToProcess.removeAll(processedFutures);
            futuresToProcess.forEach(future -> {
                processedFutures.add(future);
                try {
                    if (!((boolean) future.get())) failed.set(true);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }while(!futuresToProcess.isEmpty());

        if (!failed.get()){
            System.out.println("Successfully backed up!");
            FileStorage.saveToDisk();
        }
        else System.out.println("Back up failed");
    }

    public static ScheduledFuture<?> initiatePUTCHUNK(String fileID, Chunk chunk) {
        String dataHeader = protocolVersion + " PUTCHUNK " + peerID + " " + fileID + " " + chunk.getChunkNumber() + " " + chunk.getDesiredReplicationDegree() + " " + "\r\n" + "\r\n";
        // System.out.println(dataHeader);

        byte[] content = chunk.getContent();
        byte[] fullMessage = new byte[dataHeader.length() + (content != null ? content.length : 0)];
        System.arraycopy(dataHeader.getBytes(), 0, fullMessage,0, dataHeader.getBytes().length);
        if (content != null)
            System.arraycopy(chunk.getContent(), 0, fullMessage, dataHeader.getBytes().length, chunk.getContent().length);

        MDB.sendMessage(fullMessage);

        System.out.println("Entering Check Rep Degree -> Chunk nr. " + chunk.getChunkNumber());
        return exec.schedule(new CheckReplicationDegree(fullMessage, chunk), 1, TimeUnit.SECONDS);
    }

    public void restore(String filepath) {
        File targetFile = new File(Restore.extractFileNameFromPath(filepath));
        if (targetFile.exists() && !targetFile.isDirectory()){
            System.out.println("Already restored a file by that name ; Aborting");
            return;
        }

        System.out.println("RESTORE SERVICE -> FILE PATH = " + filepath);
        Restore.isRestoreTarget = true;


        FileObject fileObject = new FileObject(filepath);
        long fileSize = fileObject.getFile().length();
        int numberOfChunksToFind = ((int)fileSize / FileObject.MAX_CHUNK_SIZE) + 1;

        for (int i = 0; i < numberOfChunksToFind; i++) {
            byte[] messageBytes = (protocolVersion + " GETCHUNK " + peerID + " " + fileObject.getFileID() + " " + i + " " + "\r\n" + "\r\n").getBytes();

            MC.sendMessage(messageBytes);
        }

        Restore.t = exec.scheduleWithFixedDelay(() -> Restore.constructRestoredFileFromRestoredChunks(numberOfChunksToFind, filepath, fileObject.getFileID()), 100, 100, TimeUnit.MILLISECONDS);
        FileStorage.saveToDisk();
    }

    public void delete(String filepath) {
        System.out.println("DELETE SERVICE -> FILE PATH = " + filepath);

        FileObject fileObject = new FileObject(filepath);

        for (int i = 1, timeInterval = 1 ; i <= 5 ; i++, timeInterval *= 2){
            int finalI = i;
            futures.add(exec.schedule(() -> {
                byte[] messageBytes = (protocolVersion  + " DELETE " + peerID + " " + fileObject.getFileID() + " " + "\r\n" + "\r\n").getBytes();
                System.out.println("Sending Message to MC: Try " + finalI);
                MC.sendMessage(messageBytes);
            }, timeInterval, TimeUnit.SECONDS));
        }

        final Set<ScheduledFuture<?>> processedFutures = new HashSet<>();
        Set<ScheduledFuture<?>> futuresToProcess;
        do{
            futuresToProcess = new HashSet<>(futures);
            futuresToProcess.removeAll(processedFutures);
            futuresToProcess.forEach(future -> {
                processedFutures.add(future);
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }while(!futuresToProcess.isEmpty());

        if (protocolVersion.equals("1.1")){

        }
        else {
            System.out.println("Successfully deleted file!");
            fileStorage.removeInitiatedFile(fileObject);
            FileStorage.saveToDisk();
        }
    }

    public void reclaim(long spaceReclaim) {
        System.out.println("RECLAIM SERVICE -> DISK SPACE RECLAIM = " + spaceReclaim);
        if (Reclaim.checkIfNewMaxSpaceIsEnough(spaceReclaim)) {
            System.out.println("No need to delete chunks. Returning.");
            return;
        }

        String generalREMOVEDMessage = protocolVersion  + " REMOVED " + peerID + " ";
        Reclaim.deleteBackups(spaceReclaim, generalREMOVEDMessage);

        FileStorage.saveToDisk();
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