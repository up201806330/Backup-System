import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Backup {
    private static FileStorage fileStorage;

    public static void processPacketPUTCHUNK(Chunk chunk, String[] splitHeader) {
        fileStorage = FileStorage.instance;
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        // Utils.printSplitHeader(splitHeader);

        System.out.println("Processing PUTCHUNK Packet ->" + chunk.getChunkNumber());

        // check to make sure backup will not exceed that max amount of storage allowed
        if ( (chunk.getContent().length / 1000.0) + fileStorage.getCurrentlyKBytesUsedSpace() > fileStorage.getMaximumSpaceAvailable() ) {
            System.out.println("This Backup would exceed maximum allowed storage capacity");
            return;
        }

        boolean storedSuccessfully = fileStorage.storeChunk(chunk);
        if (storedSuccessfully){
            System.out.println("Sending STORED ->" + chunk.getChunkNumber());
            byte[] storedMessage = createSTORED(splitHeader);

            int rand = new Random().nextInt(401);
            Peer.getExec().schedule(() -> Peer.getMC().sendMessage(storedMessage), rand, TimeUnit.MILLISECONDS);
            FileStorage.saveToDisk();
        }
    }

    public static void processPacketSTORED(Chunk chunk, String[] splitHeader) {
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        System.out.print("Processing STORED Packet ->" + chunk.getChunkNumber());

        fileStorage.incrementReplicationDegree(chunk);
        fileStorage.updateChunksBackedPeers(chunk, Integer.parseInt(splitHeader[2]));

        System.out.println(" " + fileStorage.getPerceivedReplicationDegree(chunk));
    }

    private static byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }
}
