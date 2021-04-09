import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class TestApp {

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return ;
        }

        try {
            String peerAccessPoint = args[0];
            String operation = args[1];

            Registry registry = LocateRegistry.getRegistry();
            RemoteInterface stub = (RemoteInterface) registry.lookup(peerAccessPoint);

            // ---- Check for "exceptions" ----

            // Wrong number of args for Backup
            if ( (operation.equalsIgnoreCase("BACKUP")) && args.length != 4) {
                System.out.println("Usage: java TestApp <peer_ap> BACKUP <opnd_1> <replication_degree>");
                return ;
            }

            // Wrong number of args for state
            else if ( (operation.equalsIgnoreCase("STATE")) && args.length != 2 ) {
                System.out.println("Usage: java TestApp <peer_ap> STATE");
                return ;
            }

            // General case
            else if (args.length != 3 && !operation.equalsIgnoreCase("STATE") && !operation.equalsIgnoreCase("BACKUP")) {
                System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1>");
                return;
            }

            switch (operation.toUpperCase()) {
                case "BACKUP" -> {
                    System.out.println("Backup");
                    stub.backup(args[2], Integer.parseInt(args[3]));
                }
                case "RESTORE" -> {
                    System.out.println("Restore");
                    stub.restore(args[2]);
                }
                case "DELETE" -> {
                    System.out.println("Delete");
                    stub.delete(args[2]);
                }
                case "RECLAIM" -> {
                    System.out.println("Reclaim");
                    stub.reclaim(Integer.parseInt(args[2]));
                }
                case "STATE" -> {
                    System.out.println("State");
                    System.out.println(stub.state());
                }
                default -> System.out.println("Operation not recognized");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

    }
}
