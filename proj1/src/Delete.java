import java.io.File;

public class Delete {
    private static final FileStorage fileStorage = FileStorage.instance;

    public static void processPacketDELETE(String fileIdToDelete) {
        if (fileStorage.isFilesInitiator(FileObject.fromFileID(fileIdToDelete))){
            return;
        }

        System.out.println("Processing DELETE Packet");

        for (Chunk chunk : fileStorage.storedChunks) {
            String fileID = chunk.getFileID();

            if (fileID.equals(fileIdToDelete)) {

                // Initiator deletes entry that file was initiated
                deleteFileParser(fileID);

                // Backing peer deletes entry from stored chunks
                fileStorage.removeChunkFromStoredChunkFiles(chunk);
            }
        }

        FileStorage.saveToDisk();
    }

    private static void deleteFileParser(String fileID) {
        fileStorage.findInitiatedFile(fileID).ifPresent(fileStorage::removeInitiatedFile);
    }
}
