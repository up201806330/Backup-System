import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RepDegreeStatus {

    private boolean backupInExecution;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> currentBackup, getExistingChunks;

    public RepDegreeStatus() {
        this.backupInExecution = false;
        this.currentBackup = new ConcurrentHashMap<>();
        this.getExistingChunks = new ConcurrentHashMap<>();
    }


    public boolean isBackupInExecution() {
        return backupInExecution;
    }

    public void setBackupInExecution(boolean backupInExecution) {
        this.backupInExecution = backupInExecution;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> getCurrentBackup() {
        return currentBackup;
    }

    public void setCurrentBackup(ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> currentBackup) {
        this.currentBackup = currentBackup;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> getGetExistingChunks() {
        return getExistingChunks;
    }

    public void setGetExistingChunks(ConcurrentHashMap<String, ConcurrentHashMap<Integer, Set<Integer>>> getExistingChunks) {
        this.getExistingChunks = getExistingChunks;
    }
}
