import java.util.ArrayList;

public class Delete {
    private static final FileStorage fileStorage = FileStorage.instance;

    public static void processPacketDELETE(String fileIdToDelete) {
        if (fileStorage.isFilesInitiator(FileObject.fromFileID(fileIdToDelete))){
            return;
        }

        System.out.println("Processing DELETE Packet");

        ArrayList<Chunk> filesChunks = fileStorage.findChunkByFileID(fileIdToDelete);
        for (Chunk chunk : filesChunks){
            deleteFileParser(chunk.getFileID());
            fileStorage.removeChunkFromStoredChunkFiles(chunk);
        }

        if (filesChunks.size() > 0) FileStorage.saveToDisk();
    }

    private static void deleteFileParser(String fileID) {
        fileStorage.findInitiatedFile(fileID).ifPresent(fileStorage::removeInitiatedFile);
    }
}
