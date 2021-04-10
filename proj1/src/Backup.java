import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Backup {

    public static void processPacketPUTCHUNK(Chunk chunk, String[] splitHeader) {
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        // Utils.printSplitHeader(splitHeader);

        System.out.println("Processing PUTCHUNK Packet");

        boolean storedSuccessfully = FileStorage.storeChunk(chunk);
        if (storedSuccessfully){
            byte[] storedMessage = createSTORED(splitHeader);

            int rand = new Random().nextInt(401);
            System.out.println("Sending STORED in: " + rand + "ms");
            Peer.getExec().schedule(() -> Peer.getMC().sendMessage(storedMessage), rand, TimeUnit.MILLISECONDS);
            Peer.saveFileStorageToDisk();
        }
    }

    public static void processPacketSTORED(Chunk chunk, String[] splitHeader) {
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        System.out.println("Processing STORED Packet");

        FileStorage.incrementReplicationDegree(chunk);
        FileStorage.updateChunksBackedPeers(chunk, Integer.parseInt(splitHeader[2]));
    }

    private static byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }
}
