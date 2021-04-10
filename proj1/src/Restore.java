import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Restore {

    public static final Set<Integer> chunksAlreadySent = new HashSet<>();

    public static void processPacketGETCHUNK(String[] splitHeader) {
        System.out.println("Processing GETCHUNK Packet");

        chunksAlreadySent.clear();

        System.out.println("Number of chunk to get: " + splitHeader[4]);

        // check if has that chunk
        for (Chunk c : FileStorage.instance.getStoredChunkFiles()) {

            if (c.getFileID().equals(splitHeader[3]) && c.getChunkNumber() == Integer.parseInt(splitHeader[4])) {
                System.out.println("I have the chunk!");

                byte[] chunkMessage = createCHUNK(splitHeader, c);
                int rand = new Random().nextInt(401);
                System.out.println("Random: " + rand);
                Peer.getExec().schedule(new SendCHUNKMessage(chunkMessage, Integer.parseInt(splitHeader[4])), rand, TimeUnit.MILLISECONDS);
            }
        }
    }

    private static byte[] createCHUNK(String[] splitHeader, Chunk c) {

        String messageString = splitHeader[0] + " CHUNK " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";
        System.out.println(messageString);

        byte[] fullMessage = new byte[messageString.length() + c.getContent().length];
        System.arraycopy(messageString.getBytes(), 0, fullMessage,0, messageString.getBytes().length);
        System.arraycopy(c.getContent(), 0, fullMessage, messageString.getBytes().length, c.getContent().length);

        return fullMessage;
    }

    public static void processPacketCHUNK(Chunk newChunk, String[] splitHeader) {
        System.out.println("Processing CHUNK Packet");
        System.out.println("Size of chunksAlreadySent: " + chunksAlreadySent.size());

        // if chunk number was already sent by someone its already in the set therefore is not to be sent
        if (!chunksAlreadySent.add(Integer.parseInt(splitHeader[4])) || Peer.getId() == Integer.parseInt(splitHeader[2])) {
            System.out.println("Skipping Sending of CHUNK");
            return;
        }

        String serviceDirectory = "service-" + Peer.getId();

        FileOutputStream fos;

        try {
            System.out.println("Entering Try Catch for File");
            System.out.println("Writing Chunk Number: " + splitHeader[4]);
            System.out.println("Path of new file: " + serviceDirectory + "/restored/" + splitHeader[3]);
            // splitHeader[3] -> FileId as the name of the restored file for now
            fos = new FileOutputStream(serviceDirectory + "/restored/" + splitHeader[3], true);
            fos.write(newChunk.getContent());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Wrote to restored file chunk number " + splitHeader[4]);
    }
}
