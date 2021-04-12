import java.util.ArrayList;

public class Delete {
    private static final FileStorage fileStorage = FileStorage.instance;

    public static void processDELETE(String fileIdToDelete) {
        if (fileStorage.isFilesInitiator(FileObject.fromFileID(fileIdToDelete))){
            return;
        }

        System.out.println("Processing DELETE Packet");

        ArrayList<Chunk> filesChunks = fileStorage.findChunkByFileID(fileIdToDelete);
        for (Chunk chunk : filesChunks){
            deleteFileParser(chunk.getFileID());
            fileStorage.removeChunkFromStoredChunkFiles(chunk);
        }

        if (filesChunks.size() > 0) {
            if (Peer.protocolVersion.equals("1.1")){
                byte[] deletedMessage = ("1.1 DELETED " + Peer.getId() + " " + fileIdToDelete + " " + "\r\n" + "\r\n").getBytes();
                Peer.getMC().sendMessage(deletedMessage);
            }
            FileStorage.saveToDisk();
        }
    }

    public static void processDELETED(int senderID, String fileID) {
        if (Peer.getId() == senderID) {
            return;
        }
        if (Peer.protocolVersion.equals("1.1")){
            System.out.println("Processing DELETED ->" + fileID);
            Peer.getExec().execute(() -> FileStorage.instance.removeBackedPeerFromAllChunks(fileID, senderID));
        }
    }

    private static void deleteFileParser(String fileID) {
        fileStorage.findInitiatedFile(fileID).ifPresent(fileStorage::removeInitiatedFile);
    }

    public static void deleteDeadChunks(int senderID) {
        var deadFilesOnThisPeer = fileStorage.peersWithDeadChunks.get(senderID);
        for (Object filepath : deadFilesOnThisPeer){
            Peer.getExec().execute(() -> Peer.instance.delete((String) filepath));
        }
    }
}
