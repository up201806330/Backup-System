import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

public class FileParser {

    private final int MAX_CHUNK_SIZE = 64000;

    private final File file;
    private final String filePath;
    private final String fileID;
    private final int replicationDegree;
    private final ArrayList<Chunk> chunks;
    boolean hasExtraEmptyChunk;

    public FileParser(String filePath, int replicationDegree) {
        this.file = new File(filePath);
        this.filePath = filePath;
        this.fileID = getFileIdHashed();
        this.replicationDegree = replicationDegree;
        this.chunks = parseChunks();
        this.hasExtraEmptyChunk = checkForEmptyEndingChunk();
    }

    private ArrayList<Chunk> parseChunks() {
        byte[] chunkBuffer = new byte[MAX_CHUNK_SIZE];
        int currentChunkNumber = 0;

        ArrayList<Chunk> allChunks = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(file);

            int size;
            while( (size = fis.read(chunkBuffer)) > 0) {
                Chunk createdChunk = new Chunk(fileID, ++currentChunkNumber, replicationDegree, Arrays.copyOf(chunkBuffer, size));
                allChunks.add(createdChunk);

                chunkBuffer = new byte[MAX_CHUNK_SIZE];
            }

            if (this.hasExtraEmptyChunk) {
                Chunk chunk = new Chunk(fileID, ++currentChunkNumber, replicationDegree);
                this.chunks.add(chunk);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return allChunks;
    }

    private String getFileIdHashed() {

        // using file name, date modified and owner as suggested in handout
        String idToHash = file.getName() + file.lastModified() + file.getParent();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(idToHash.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (byte aHash : hash) {
                String hex = Integer.toHexString(0xff & aHash);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkForEmptyEndingChunk() {
        return ((this.file.length() % MAX_CHUNK_SIZE) == 0);
    }

    public File getFile() {
        return file;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileID() {
        return fileID;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public ArrayList<Chunk> getChunks() {
        return chunks;
    }
}
