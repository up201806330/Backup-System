import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Delete {

    public static void processPacketDELETE(String fileIdToDelete) {
        System.out.println("Processing DELETE Packet");

        System.out.println("FileId To Delete: " + fileIdToDelete);
        System.out.println("\n");

        var fileStorage = FileStorage.instance;

        // iterate over each of the 3 (getChunkMap, getBackedUpFiles, getStoredChunkFiles)
        // and eliminate the corresponding entries.
        // Delete also the files themselves.

        for (ConcurrentHashMap.Entry<Chunk, String> entry : fileStorage.getChunkMap().entrySet()) {
            Chunk chunk = entry.getKey();
            String value = entry.getValue();

//            String chunkString = entry.getKey().toString();
//            System.out.println(chunkString);
//            System.out.println("---");
//            System.out.println(value);

            if (value.equals(fileIdToDelete)) {
                System.out.println("Found Entry to Delete!");

                System.out.println("Nr of BackedUpFiles Pre: " + fileStorage.getBackedUpFiles().size());
                // Delete FileParser object entry in Set backedUpFiles
                deleteFileParser(fileStorage, value);
                System.out.println("Nr of BackedUpFiles Pos: " + fileStorage.getBackedUpFiles().size());

                System.out.println("Nr of Entries in storedChunkFiles Pre: " + fileStorage.getStoredChunkFiles().size());
                // Delete Chunk entry in Set of storedChunkFiles
                deleteChunkFromSet(fileStorage, chunk);
                System.out.println("Nr of Entries in storedChunkFiles Pos: " + fileStorage.getStoredChunkFiles().size());

                // Delete Backup file itself
                deleteFileViaName(chunk.getChunkID());

                System.out.println("Nr of Entries in storedChunkFiles Pos: " + fileStorage.getChunkMap().entrySet().size());
                // Delete entry in ConcurrentHashMap
                fileStorage.removeEntryFromChunkMap(chunk);
                System.out.println("Nr of Entries in storedChunkFiles Pos: " + fileStorage.getChunkMap().entrySet().size());

            }

        }
    }

    private static void deleteFileParser(FileStorage fileStorage, String fileID) {
        fileStorage.findBackedUpFile(fileID).ifPresent(fileStorage::removeFileParserFromBackedUpFiles);
    }

    private static void deleteChunkFromSet(FileStorage fileStorage, Chunk chunk) {
        for (Chunk c : fileStorage.getStoredChunkFiles()) {
            if (c.equals(chunk)) {
                System.out.println("Found chunk to delete from set");
                fileStorage.removeChunkFromStoredChunkFiles(c);
            }
        }
    }

    private static void deleteFileViaName(String filepath) {
        String newPath = "/service-" + Peer.getId() + "/chunks/" + filepath;
        File file = new File(newPath);

        if (file.delete()) {
            System.out.println("File deleted with success");
        }
        else {
            System.out.println("File not deleted");
        }
    }

}
