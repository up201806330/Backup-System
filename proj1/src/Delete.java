import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class Delete {
    private static FileStorage fileStorage;

    public static void processPacketDELETE(String fileIdToDelete) {
        fileStorage = FileStorage.instance;
        if (fileStorage.isFilesInitiator(FileParser.fromFileID(fileIdToDelete))){
            return;
        }

        System.out.println("Processing DELETE Packet");

        for (Chunk chunk : fileStorage.storedChunkFiles) {
            String fileID = chunk.getFileID();

            if (fileID.equals(fileIdToDelete)) {

                // Delete FileParser object entry in Set backedUpFiles
                deleteFileParser(fileID);

                // Delete Chunk entry in Set of storedChunkFiles
                deleteChunkFromSet(chunk);

                // Delete Backup file itself
                deleteFileViaName(chunk.getChunkID());
            }
        }

        FileStorage.saveToDisk();
    }

    private static void deleteFileParser(String fileID) {
        fileStorage.findInitiatedFile(fileID).ifPresent(fileStorage::removeInitiatedFile);
    }

    private static void deleteChunkFromSet(Chunk chunk) {
        for (Chunk c : fileStorage.storedChunkFiles) {
            if (c.equals(chunk)) {
                fileStorage.removeChunkFromStoredChunkFiles(c);
            }
        }
    }

    private static void deleteFileViaName(String filepath) {
        String newPath = fileStorage.chunksDir + "/" + filepath;
        File file = new File(newPath);

        String fileName = "Chunk file nr. " + filepath.substring(filepath.length() - 1);
        if (file.delete()) System.out.println(fileName + " deleted with success");
        else System.out.println(fileName + " not deleted");
    }

}
