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
            String key = entry.getKey().toString();
            String value = entry.getValue();

            System.out.println(key);
            System.out.println("---");
            System.out.println(value);

//            String[] separatedValue = value;
        }

        // isChunkOfFileId(fileIdToDelete, fileIdToDelete-chunkNr)

    }
}
