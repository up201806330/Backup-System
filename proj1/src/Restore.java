import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Restore {

    public static final Set<Integer> chunksAlreadySent = new HashSet<>();
    public static ScheduledFuture<?> t;

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

        FileOutputStream fos;

        try {
            System.out.println("Writing Chunk Number: " + splitHeader[4]);
            // splitHeader[3] -> FileId as the name of the restored file for now
            fos = new FileOutputStream(FileStorage.instance.cacheDir + "/" + splitHeader[3] + "-" + splitHeader[4], true);
            fos.write(newChunk.getContent());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Wrote to restored file chunk number " + splitHeader[4]);
    }

    public static void constructRestoredFileFromRestoredChunks(int numberOfChunksToFind, String filepath, String fileID) {
        System.out.println("Running constructRestoredFileFromRestoredChunks");
        if (chunksAlreadySent.size() != numberOfChunksToFind) return;

        System.out.println("GOT ALL CHUNKS TO RESTORE FILE");
        t.cancel(false);

        // constructs restored file
        try {
            System.out.println("Saving to " + FileStorage.instance.restoreDir + "/" + extractFileNameFromPath(filepath));
            FileOutputStream fos = new FileOutputStream(FileStorage.instance.restoreDir + "/" + extractFileNameFromPath(filepath), true);
            byte[] buf = new byte[FileParser.MAX_CHUNK_SIZE];
            for(int i = 1 ; i <= numberOfChunksToFind ; i++){
                System.out.println("Getting Chunk From " + Paths.get(fileID + "-" + i));
                InputStream fis = Files.newInputStream(Paths.get(FileStorage.instance.cacheDir + "/" + fileID + "-" + i), StandardOpenOption.DELETE_ON_CLOSE);
                int b;
                while ( (b = fis.read(buf)) >= 0)
                    fos.write(buf, 0, b);
                fis.close();
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Finishing constructRestoredFileFromRestoredChunks");
    }

    public static String extractFileNameFromPath(String path){
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }
}
