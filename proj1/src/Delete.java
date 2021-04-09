import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Delete {

    public static void processPacketDELETE(String fileIdToDelete) {
        System.out.println("Processing DELETE Packet");

        var fileStorage = FileStorage.instance;

        for (ConcurrentHashMap.Entry<Chunk, String> entry : fileStorage.getChunkMap().entrySet()) {
            Chunk chunk = entry.getKey();
            String value = entry.getValue();

            if (value.equals(fileIdToDelete)) {

                // Delete FileParser object entry in Set backedUpFiles
                deleteFileParser(fileStorage, value);

                // Delete Chunk entry in Set of storedChunkFiles
                deleteChunkFromSet(fileStorage, chunk);

                // Delete Backup file itself
                deleteFileViaName(chunk.getChunkID());

                // Delete entry in ConcurrentHashMap
                fileStorage.removeEntryFromChunkMap(chunk);
            }
        }
    }

    private static void deleteFileParser(FileStorage fileStorage, String fileID) {
        fileStorage.findBackedUpFile(fileID).ifPresent(fileStorage::removeFileParserFromBackedUpFiles);
    }

    private static void deleteChunkFromSet(FileStorage fileStorage, Chunk chunk) {
        for (Chunk c : fileStorage.getStoredChunkFiles()) {
            if (c.equals(chunk)) {
                fileStorage.removeChunkFromStoredChunkFiles(c);
            }
        }
    }

    private static void deleteFileViaName(String filepath) {
        String newPath = FileStorage.instance.chunksDir + "/" + filepath;
        File file = new File(newPath);

        if (file.delete()) System.out.println("File deleted with success");
        else System.out.println("File not deleted");
    }

}
