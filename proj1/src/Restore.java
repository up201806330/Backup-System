import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Restore {

    public static final Set<Integer> chunksAlreadySent = new HashSet<>();
    public static ScheduledFuture<?> t;

    public static void processPacketGETCHUNK(String[] splitHeader) {
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        System.out.println("Processing GETCHUNK Packet");

        chunksAlreadySent.clear();

        Chunk chunk = new Chunk(splitHeader[3], Integer.parseInt(splitHeader[4]));
        FileStorage.instance.hasChunkBackedUp(chunk).map(c ->  {
            byte[] chunkMessage = createCHUNK(splitHeader, c);

            int rand = new Random().nextInt(401);
            Peer.getExec().schedule(new SendCHUNKMessage(chunkMessage, Integer.parseInt(splitHeader[4])), rand, TimeUnit.MILLISECONDS);
            return c;
        });
    }

    private static byte[] createCHUNK(String[] splitHeader, Chunk c) {
        String messageString = splitHeader[0] + " CHUNK " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        byte[] fullMessage = new byte[messageString.length() + c.getContent().length];
        System.arraycopy(messageString.getBytes(), 0, fullMessage,0, messageString.getBytes().length);
        System.arraycopy(c.getContent(), 0, fullMessage, messageString.getBytes().length, c.getContent().length);

        return fullMessage;
    }

    public static void processPacketCHUNK(Chunk newChunk, String[] splitHeader) {
        // if chunk number was already sent by someone its already in the set therefore is not to be sent
        if (!chunksAlreadySent.add(Integer.parseInt(splitHeader[4])) || Peer.getId() == Integer.parseInt(splitHeader[2])) {
            return;
        }

        System.out.println("Processing CHUNK Packet");

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(FileStorage.instance.cacheDir + "/" + splitHeader[3] + "-" + splitHeader[4], true);
            fos.write(newChunk.getContent());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void constructRestoredFileFromRestoredChunks(int numberOfChunksToFind, String filepath, String fileID) {
        if (chunksAlreadySent.size() != numberOfChunksToFind) return;

        System.out.print("GOT ALL CHUNKS TO RESTORE FILE...");
        t.cancel(false);

        // constructs restored file
        try {
            FileOutputStream fos = new FileOutputStream(FileStorage.instance.restoreDir + "/" + extractFileNameFromPath(filepath), true);
            byte[] buf = new byte[FileObject.MAX_CHUNK_SIZE];
            for(int i = 0 ; i < numberOfChunksToFind ; i++){
                InputStream fis = Files.newInputStream(Paths.get(FileStorage.instance.cacheDir + "/" + fileID + "-" + i), StandardOpenOption.DELETE_ON_CLOSE);
                int b;
                while ( (b = fis.read(buf)) >= 0)
                    fos.write(buf, 0, b);
                fis.close();
            }
            fos.close();
            System.out.println(" RESTORED!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String extractFileNameFromPath(String path){
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
