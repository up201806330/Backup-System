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

        System.out.println("Processing PUTCHUNK Packet");

        boolean storedSuccessfully = fileStorage.storeChunk(chunk);
        if (storedSuccessfully){
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

        System.out.println("Processing STORED Packet");

        fileStorage.incrementReplicationDegree(chunk);
        fileStorage.updateChunksBackedPeers(chunk, Integer.parseInt(splitHeader[2]));
    }

    private static byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }
}
