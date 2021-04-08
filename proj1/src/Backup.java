import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Backup {

    public static void processPacketPUTCHUNK(Chunk chunk, String[] splitHeader) {
        System.out.println("Processing PUTCHUNK Packet");

        var fileStorage = FileStorage.instance;

        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            System.out.println("SKIPPING PUTCHUNK");
            return ;
        }
        // Utils.printSplitHeader(splitHeader);

        boolean storedSuccessfully = fileStorage.storeChunk(chunk);
        if (storedSuccessfully){
            byte[] storedMessage = createSTORED(splitHeader);


            int rand = new Random().nextInt(401);
            System.out.println("Sending STORED in: " + rand + "ms");
            Peer.getExec().schedule(() -> Peer.getMC().sendMessage(storedMessage), rand, TimeUnit.MILLISECONDS);
        }
    }

    public static void processPacketSTORED(String[] splitHeader) {
        System.out.println("Processing STORED Packet");

        FileStorage.instance.incrementReplicationDegree(splitHeader[3] + "-" + splitHeader[4]);
    }

    private static byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + splitHeader[2] + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }
}
