import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {

    void backup(String filePathname, int replicationDegree) throws RemoteException;
    void restore(String filePathname) throws RemoteException;
    void delete(String filePathname) throws RemoteException;
    void reclaimStorage(int maxDiskSpaceToStore) throws RemoteException;
    String retrieveState() throws RemoteException;
}
