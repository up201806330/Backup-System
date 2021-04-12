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
    private static final FileStorage fileStorage = FileStorage.instance;
    public static boolean isRestoreTarget;
    public static final Set<Integer> chunksAlreadySent = new HashSet<>();
    public static ScheduledFuture<?> t;

    public static void processGETCHUNK(String[] splitHeader) {
        if (Peer.getId() == Integer.parseInt(splitHeader[2])) {
            return ;
        }

        System.out.println("Processing GETCHUNK Packet");

        chunksAlreadySent.clear();

        Chunk chunk = new Chunk(splitHeader[3], Integer.parseInt(splitHeader[4]));
        fileStorage.hasChunkBackedUp(chunk).map(c ->  {
            byte[] chunkMessage = createCHUNK(splitHeader, c);

            int rand = new Random().nextInt(401);
            Peer.getExec().schedule(() -> sendCHUNKMessage(chunkMessage, Integer.parseInt(splitHeader[4])), rand, TimeUnit.MILLISECONDS);
            return c;
        });
    }

    private static byte[] createCHUNK(String[] splitHeader, Chunk c) {
        String messageString = "1.0 CHUNK " + Peer.getId() + " " + splitHeader[3] + " " + splitHeader[4] + " " + "\r\n" + "\r\n";

        byte[] fullMessage = new byte[messageString.length() + c.getContent().length];
        System.arraycopy(messageString.getBytes(), 0, fullMessage,0, messageString.getBytes().length);
        System.arraycopy(c.getContent(), 0, fullMessage, messageString.getBytes().length, c.getContent().length);

        return fullMessage;
    }

    private static void sendCHUNKMessage(byte[] chunkMessage, int chunkNumber){
        if (Restore.chunksAlreadySent.contains(chunkNumber)) return;
        Peer.getMDR().sendMessage(chunkMessage);
    }

    public static void processCHUNK(Chunk newChunk, String[] splitHeader) {
        if (!chunksAlreadySent.add(Integer.parseInt(splitHeader[4])) || // If chunk was already sent by someone else
                Peer.getId() == Integer.parseInt(splitHeader[2]) ||     // Or this peer sent this message
                !isRestoreTarget) {                                     // Or this peer isn't the one who requested the RESTORE
            return;
        }

        System.out.println("Processing CHUNK Packet");

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileStorage.cacheDir + "/" + splitHeader[3] + "-" + splitHeader[4], true);
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
            FileOutputStream fos = new FileOutputStream(fileStorage.restoreDir + "/" + extractFileNameFromPath(filepath), true);
            byte[] buf = new byte[FileObject.MAX_CHUNK_SIZE];
            for(int i = 0 ; i < numberOfChunksToFind ; i++){
                InputStream fis = Files.newInputStream(Paths.get(fileStorage.cacheDir + "/" + fileID + "-" + i), StandardOpenOption.DELETE_ON_CLOSE);
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
