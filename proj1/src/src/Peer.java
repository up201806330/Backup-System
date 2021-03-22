import java.rmi.RemoteException;

public class Peer implements RemoteInterface {

    private int id;
    private static ControlChannel MC;
    private static BackupChannel MDB;
    private static RestoreChannel MDR;


    public static void main(String[] args) {

    }



    @Override
    public void backup(String filePathname, int replicationDegree) throws RemoteException {

    }

    @Override
    public void restore(String filePathname) throws RemoteException {

    }

    @Override
    public void delete(String filePathname) throws RemoteException {

    }

    @Override
    public void reclaimStorage(int maxDiskSpaceToStore) throws RemoteException {

    }

    @Override
    public void retrieveState() throws RemoteException {

    }
}
