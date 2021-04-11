import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Backup {
    private static FileStorage fileStorage;

    public static synchronized void processPacketPUTCHUNK(Chunk chunk, String[] splitHeader) {
        fileStorage = FileStorage.instance;
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) || fileStorage.isChunksInitiator(chunk)) {
            return ;
        }

        System.out.println("Processing PUTCHUNK Packet ->" + chunk.getChunkNumber());

        // Check to make sure backup will not exceed that max amount of storage allowed
        if ((chunk.getContent().length / 1000.0) + fileStorage.getCurrentlyKBytesUsedSpace() > fileStorage.getMaximumSpaceAvailable()) {
            System.out.println("This Backup would exceed maximum allowed storage capacity");
            return;
        }

        byte[] storedMessage = createSTORED(splitHeader);

        int rand = new Random().nextInt(401);
        var storedSuccessfullyFuture = Peer.getExec().schedule(
                () -> sendSTOREDMessage(storedMessage, chunk), rand, TimeUnit.MILLISECONDS);
        try {
            storedSuccessfullyFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("Error in thread in charge of storing");
        }
        FileStorage.saveToDisk();
    }

    private static void sendSTOREDMessage(byte[] storedMessage, Chunk chunk) {
        if (Peer.protocolVersion.equals("1.1")){ // Versions after 1.0 try to avoid storing already replicated enough chunks
            System.out.println(FileStorage.instance.getPerceivedReplicationDegree(chunk) + " ; " + chunk.getDesiredReplicationDegree());
            if (FileStorage.instance.getPerceivedReplicationDegree(chunk) >= chunk.getDesiredReplicationDegree()){
                System.out.println("Chunk was already backed up enough");
                return;
            }
        }

        fileStorage.storeChunk(chunk);
        Peer.getMC().sendMessage(storedMessage);
        System.out.println("Sending STORED ->" + chunk.getChunkNumber());
    }

    public static void processPacketSTORED(Chunk chunk, String[] splitHeader) {
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        System.out.println("Processing STORED Packet ->" + chunk.getChunkNumber());

        fileStorage.incrementReplicationDegree(chunk);
        fileStorage.updateChunksBackedPeers(chunk, Integer.parseInt(splitHeader[2]));
    }

    private static byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }
}
