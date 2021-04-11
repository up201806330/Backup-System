import java.io.File;

public class Delete {
    private static FileStorage fileStorage;

    public static void processPacketDELETE(String fileIdToDelete) {
        fileStorage = FileStorage.instance;
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

                // Backing peer deletes actual chunk file
                deleteFileViaName(chunk.getChunkID());
            }
        }

        FileStorage.saveToDisk();
    }

    private static void deleteFileParser(String fileID) {
        fileStorage.findInitiatedFile(fileID).ifPresent(fileStorage::removeInitiatedFile);
    }

    public static void deleteFileViaName(String filepath) {
        fileStorage = FileStorage.instance;
        if (fileStorage==null) {
            System.out.println("filestorage instance is null");
        }
        else if (fileStorage.chunksDir==null){
            System.out.println("chunksDir value is null :(");
        }
        String newPath = fileStorage.chunksDir + "/" + filepath;
        File file = new File(newPath);

        String fileName = "Chunk file nr. " + filepath.substring(filepath.length() - 1);
        if (file.delete()) System.out.println(fileName + " deleted with success");
        else System.out.println(fileName + " not deleted");
    }

}
