import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Backup {

    public static void processPacketPUTCHUNK(String[] splitHeader, String bodyString) {
        System.out.println("Processing PUTCHUNK Packet");

        var fileStorage = FileStorage.instance;

        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            System.out.println("SKIPPING PUTCHUNK");
            return ;
        }
        Utils.printSplitHeader(splitHeader);


        Chunk newChunk = new Chunk(Integer.parseInt(splitHeader[4]), bodyString.getBytes(), splitHeader[3], bodyString.getBytes().length);

        boolean storedSuccessfully = fileStorage.storeChunk(newChunk);

        if (storedSuccessfully){
            byte[] storedMessage = createSTORED(splitHeader);
            Peer.getMC().sendMessage(storedMessage);
        }

    }

    public static void processPacketSTORED(String[] splitHeader) {
        System.out.println("Processing STORED Packet");

    }

    private static byte[] createSTORED(String[] splitHeader) {
        String storedString = splitHeader[0] + " STORED " + splitHeader[2] + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        return storedString.getBytes();
    }
}
