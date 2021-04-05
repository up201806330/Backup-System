import java.rmi.RemoteException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Peer implements RemoteInterface {

    private int peerId;
    private static Channel MC;
    private static Channel MDB;
    private static Channel MDR;

    private ScheduledThreadPoolExecutor executor;


    public static void main(String[] args) {

        if (args.length != 9) {
            System.out.println("Usage: ");
            return ;
        }


    }

    public Peer() {

    }

    @Override
    public void backup(String filePathname, int replicationDegree) throws RemoteException {
        System.out.println("Entering Backup of " + filePathname + " with replic degree of " + replicationDegree);
    }

    @Override
    public void restore(String filePathname) throws RemoteException {
        System.out.println("Entering Restore of " + filePathname);
    }

    @Override
    public void delete(String filePathname) throws RemoteException {
        System.out.println("Entering Delete of " + filePathname);
    }

    @Override
    public void reclaimStorage(int maxDiskSpaceToStore) throws RemoteException {
        System.out.println("Entering Reclaim with disk space reclaim of " + maxDiskSpaceToStore);
    }

    @Override
    public void retrieveState() throws RemoteException {
        System.out.println("Entering Retrieve of State");
    }
}
